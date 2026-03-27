import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
const FIREBASE_PROJECT_ID = "nexohogar-72649"
const FIREBASE_CLIENT_EMAIL = Deno.env.get("FIREBASE_CLIENT_EMAIL")!
const FIREBASE_PRIVATE_KEY = Deno.env.get("FIREBASE_PRIVATE_KEY")!
const WEBHOOK_SECRET = Deno.env.get("WEBHOOK_SECRET")!

// Genera un access token de Firebase usando la service account (FCM v1)
async function getFirebaseAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000)

  const toB64Url = (obj: object | string) => {
    const str = typeof obj === "string" ? obj : JSON.stringify(obj)
    return btoa(str).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_")
  }

  const header = toB64Url({ alg: "RS256", typ: "JWT" })
  const payload = toB64Url({
    iss: FIREBASE_CLIENT_EMAIL,
    sub: FIREBASE_CLIENT_EMAIL,
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  })

  const signingInput = `${header}.${payload}`

  const privateKeyPem = FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n")
  const pemContents = privateKeyPem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "")

  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0))

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  )

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signingInput)
  )

  const signatureB64 = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")

  const jwt = `${signingInput}.${signatureB64}`

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  })

  const tokenData = await tokenResponse.json()
  if (!tokenData.access_token) {
    throw new Error(`Firebase token error: ${JSON.stringify(tokenData)}`)
  }
  return tokenData.access_token
}

serve(async (req) => {
  // Validar secret interno
  const secret = req.headers.get("x-webhook-secret")
  if (secret !== WEBHOOK_SECRET) {
    return new Response("Unauthorized", { status: 401 })
  }

  const { household_id, requester_user_id } = await req.json()
  if (!household_id || !requester_user_id) {
    return new Response("Missing params", { status: 400 })
  }

  const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

  // Obtener nombre del solicitante y nombre del hogar en paralelo
  const [householdResult, requesterResult] = await Promise.all([
    supabase.from("households").select("name").eq("id", household_id).single(),
    supabase.auth.admin.getUserById(requester_user_id),
  ])

  const requesterName =
    requesterResult.data?.user?.user_metadata?.full_name ||
    requesterResult.data?.user?.email ||
    "Alguien"
  const householdName = householdResult.data?.name || "tu hogar"

  // Obtener admins activos del hogar
  const { data: adminMembers } = await supabase
    .from("household_members")
    .select("user_id")
    .eq("household_id", household_id)
    .eq("role", "admin")
    .eq("status", "accepted")

  if (!adminMembers || adminMembers.length === 0) {
    console.log("No hay admins en el hogar:", household_id)
    return new Response(JSON.stringify({ message: "No admin found" }), { status: 200 })
  }

  const adminUserIds = adminMembers.map((m: { user_id: string }) => m.user_id)

  // Obtener tokens FCM de los admins
  const { data: fcmTokens } = await supabase
    .from("fcm_tokens")
    .select("token, device_name")
    .in("user_id", adminUserIds)

  if (!fcmTokens || fcmTokens.length === 0) {
    console.log("No hay FCM tokens para los admins")
    return new Response(JSON.stringify({ message: "No FCM tokens found" }), { status: 200 })
  }

  const accessToken = await getFirebaseAccessToken()

  // Enviar notificación a cada token
  const results = await Promise.all(
    fcmTokens.map(async ({ token, device_name }: { token: string; device_name: string }) => {
      const res = await fetch(
        `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token,
              notification: {
                title: "🏠 Nueva solicitud de ingreso",
                body: `${requesterName} quiere unirse a ${householdName}`,
              },
              data: {
                type: "join_request",
                household_id,
              },
              android: {
                priority: "high",
              },
            },
          }),
        }
      )
      const json = await res.json()
      console.log(`Token (${device_name}):`, JSON.stringify(json))
      return { device_name, ...json }
    })
  )

  return new Response(
    JSON.stringify({ sent: results.length, results }),
    { status: 200, headers: { "Content-Type": "application/json" } }
  )
})
