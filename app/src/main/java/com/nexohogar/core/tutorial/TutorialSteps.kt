package com.nexohogar.core.tutorial

/**
 * Definición de pasos del tutorial para cada módulo.
 * Los targetTag deben coincidir con los Modifier.testTag() en las pantallas.
 */
object TutorialSteps {

    fun getSteps(module: TutorialModule): List<TutorialStep> = when (module) {
        TutorialModule.DASHBOARD -> listOf(
            TutorialStep(
                title = "Balance General",
                description = "Aquí puedes ver el balance total de todas tus cuentas del hogar.",
                targetTag = "dashboard_balance"
            ),
            TutorialStep(
                title = "Resumen de Gastos",
                description = "Este gráfico muestra la distribución de tus gastos por categoría.",
                targetTag = "dashboard_chart"
            ),
            TutorialStep(
                title = "Últimas Transacciones",
                description = "Las transacciones más recientes de tu hogar aparecen aquí.",
                targetTag = "dashboard_recent"
            )
        )

        TutorialModule.ACCOUNTS -> listOf(
            TutorialStep(
                title = "Lista de Cuentas",
                description = "Aquí ves todas las cuentas financieras de tu hogar: efectivo, bancos, etc.",
                targetTag = "accounts_list"
            ),
            TutorialStep(
                title = "Agregar Cuenta",
                description = "Presiona este botón para crear una nueva cuenta financiera.",
                targetTag = "accounts_add_button"
            ),
            TutorialStep(
                title = "Detalle de Cuenta",
                description = "Toca cualquier cuenta para ver su detalle y movimientos.",
                targetTag = "accounts_item"
            )
        )

        TutorialModule.TRANSACTIONS -> listOf(
            TutorialStep(
                title = "Historial de Transacciones",
                description = "Todas las transacciones del hogar ordenadas por fecha.",
                targetTag = "transactions_list"
            ),
            TutorialStep(
                title = "Nueva Transacción",
                description = "Presiona aquí para registrar un nuevo ingreso o gasto.",
                targetTag = "transactions_add_button"
            ),
            TutorialStep(
                title = "Filtros",
                description = "Filtra transacciones por cuenta, categoría o fecha.",
                targetTag = "transactions_filters"
            )
        )

        TutorialModule.BUDGETS -> listOf(
            TutorialStep(
                title = "Presupuestos Activos",
                description = "Controla cuánto has gastado vs. tu presupuesto en cada categoría.",
                targetTag = "budgets_list"
            ),
            TutorialStep(
                title = "Crear Presupuesto",
                description = "Define un nuevo presupuesto para controlar tus gastos.",
                targetTag = "budgets_add_button"
            )
        )

        TutorialModule.INVENTORY -> listOf(
            TutorialStep(
                title = "Productos del Hogar",
                description = "Gestiona los productos y artículos de tu hogar.",
                targetTag = "inventory_list"
            ),
            TutorialStep(
                title = "Agregar Producto",
                description = "Agrega un nuevo producto. Puedes usar el escáner de código de barras.",
                targetTag = "inventory_add_button"
            ),
            TutorialStep(
                title = "Movimientos",
                description = "Registra entradas y salidas de productos.",
                targetTag = "inventory_movements"
            )
        )

        TutorialModule.RECURRING_BILLS -> listOf(
            TutorialStep(
                title = "Gastos Recurrentes",
                description = "Aquí ves todos tus pagos periódicos: servicios, suscripciones, etc.",
                targetTag = "recurring_list"
            ),
            TutorialStep(
                title = "Nuevo Gasto Recurrente",
                description = "Agrega un nuevo pago periódico para no olvidar ningún vencimiento.",
                targetTag = "recurring_add_button"
            )
        )

        TutorialModule.HOUSEHOLD -> listOf(
            TutorialStep(
                title = "Tu Hogar",
                description = "Información general de tu hogar y opciones de configuración.",
                targetTag = "household_info"
            ),
            TutorialStep(
                title = "Miembros",
                description = "Administra los miembros del hogar. Invita, acepta o elimina miembros.",
                targetTag = "household_members"
            ),
            TutorialStep(
                title = "Código de Invitación",
                description = "Comparte este código para que otros se unan a tu hogar.",
                targetTag = "household_invite"
            )
        )

        TutorialModule.INVITE_MEMBER -> listOf(
            TutorialStep(
                title = "Código de Invitación",
                description = "Este es el código único de tu hogar. Compártelo con quien quieras invitar.",
                targetTag = "invite_code"
            ),
            TutorialStep(
                title = "Compartir",
                description = "Toca este botón para enviar el código por WhatsApp, mensaje u otra app.",
                targetTag = "invite_share"
            ),
            TutorialStep(
                title = "¿Cómo se une?",
                description = "La otra persona ingresa el código en su app para unirse a tu hogar.",
                targetTag = "invite_join"
            )
        )
    }
}