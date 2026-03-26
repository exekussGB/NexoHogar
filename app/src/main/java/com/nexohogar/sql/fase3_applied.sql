-- ============================================================
-- FASE 3 — SQL aplicado en Supabase (NexoHogar)
-- Proyecto: fpsdkpurugviwygfuljp
-- Fecha: 2026-03-25
-- ============================================================

-- 1. FIX rpc_transfer: validaciones + transaction_entries (double-entry)
CREATE OR REPLACE FUNCTION public.rpc_transfer(
    household_id uuid, 
    from_account_id uuid, 
    to_account_id uuid, 
    amount_clp numeric, 
    description text DEFAULT NULL, 
    transaction_date date DEFAULT CURRENT_DATE
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
    v_id uuid;
    v_user uuid;
BEGIN
    v_user := auth.uid();
    
    IF from_account_id = to_account_id THEN
        RAISE EXCEPTION 'Las cuentas origen y destino deben ser diferentes';
    END IF;
    
    IF amount_clp <= 0 THEN
        RAISE EXCEPTION 'El monto debe ser positivo';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM household_members 
        WHERE household_members.household_id = rpc_transfer.household_id 
        AND user_id = v_user
    ) THEN
        RAISE EXCEPTION 'Unauthorized: user does not belong to this household';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM accounts 
        WHERE id = from_account_id AND accounts.household_id = rpc_transfer.household_id
    ) THEN
        RAISE EXCEPTION 'Source account does not belong to this household';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM accounts 
        WHERE id = to_account_id AND accounts.household_id = rpc_transfer.household_id
    ) THEN
        RAISE EXCEPTION 'Destination account does not belong to this household';
    END IF;

    INSERT INTO transactions (
        household_id, created_by, type, account_id, to_account_id,
        amount_clp, amount_original, exchange_rate_to_clp, currency_code,
        description, transaction_date, status
    ) VALUES (
        rpc_transfer.household_id, v_user, 'transfer', from_account_id, to_account_id,
        amount_clp, amount_clp, 1.0, 'CLP',
        description, transaction_date, 'posted'
    )
    RETURNING id INTO v_id;

    INSERT INTO transaction_entries (transaction_id, account_id, entry_type, amount_clp)
    VALUES
        (v_id, to_account_id, 'debit', amount_clp),
        (v_id, from_account_id, 'credit', amount_clp);

    UPDATE accounts SET balance = balance - amount_clp WHERE id = from_account_id;
    UPDATE accounts SET balance = balance + amount_clp WHERE id = to_account_id;

    RETURN v_id;
END;
$function$;

-- 2. Vista con nombre del creador
CREATE OR REPLACE VIEW v_transactions_with_user AS
SELECT 
    t.*,
    COALESCE(u.raw_user_meta_data->>'display_name', u.raw_user_meta_data->>'full_name', u.email) as created_by_name
FROM transactions t
LEFT JOIN auth.users u ON u.id = t.created_by;
