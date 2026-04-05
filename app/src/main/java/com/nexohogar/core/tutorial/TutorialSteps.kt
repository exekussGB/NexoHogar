package com.nexohogar.core.tutorial

/**
 * Definición de pasos del tutorial para cada módulo.
 * Los targetTag deben coincidir con los Modifier.testTag() en las pantallas.
 *
 * ACTUALIZADO: Incluye pasos para las 8 mejoras v2.
 */
object TutorialSteps {

    fun getSteps(module: TutorialModule): List<TutorialStep> = when (module) {
        TutorialModule.DASHBOARD -> listOf(
            TutorialStep(
                title = "Balance General",
                description = "Aquí puedes ver el balance total de todas tus cuentas del hogar. Ahora se calcula en tiempo real sumando los saldos reales de cada cuenta.",
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
                description = "Aquí ves todas las cuentas financieras de tu hogar organizadas por tipo: bancarias, ahorro, tarjetas, efectivo y otros. Cada una muestra un ícono según su categoría.",
                targetTag = "accounts_list"
            ),
            TutorialStep(
                title = "Desliza para gestionar",
                description = "Desliza una cuenta hacia la derecha para editarla o hacia la izquierda para eliminarla. Sentirás una vibración al cruzar el umbral.",
                targetTag = "accounts_item"
            ),
            TutorialStep(
                title = "Agregar Cuenta",
                description = "Presiona este botón para crear una nueva cuenta financiera. Elige el tipo y se asignará automáticamente su ícono.",
                targetTag = "accounts_add_button"
            )
        )

        TutorialModule.TRANSACTIONS -> listOf(
            TutorialStep(
                title = "Historial de Transacciones",
                description = "Todas las transacciones del hogar ordenadas por fecha.",
                targetTag = "transactions_list"
            ),
            TutorialStep(
                title = "Desliza para gestionar",
                description = "Desliza un movimiento hacia la derecha para abrir directamente en modo edición. Desliza a la izquierda para ver el detalle. El gesto es más sensible y con feedback háptico.",
                targetTag = "transactions_swipe"
            ),
            TutorialStep(
                title = "Filtro por Fecha",
                description = "Toca el ícono de calendario para filtrar por rango de fechas. Usa atajos rápidos como Hoy, Esta semana, Este mes o selecciona un período personalizado.",
                targetTag = "transactions_date_filter"
            ),
            TutorialStep(
                title = "Nueva Transacción",
                description = "Presiona aquí para registrar un nuevo ingreso o gasto. Recuerda que la descripción es obligatoria para identificar cada movimiento.",
                targetTag = "transactions_add_button"
            ),
            TutorialStep(
                title = "Filtros por Tipo",
                description = "Filtra transacciones por tipo (Ingresos, Gastos, Transferencias). Combina con el filtro de fecha para búsquedas precisas.",
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
                title = "Categoría del Gasto",
                description = "Asigna una categoría al crear o editar un gasto recurrente. Al marcarlo como pagado, la transacción heredará esa categoría para tus presupuestos.",
                targetTag = "recurring_category"
            ),
            TutorialStep(
                title = "Nuevo Gasto Recurrente",
                description = "Agrega un nuevo pago periódico para no olvidar ningún vencimiento.",
                targetTag = "recurring_add_button"
            ),
            TutorialStep(
                title = "Historial de Pagos",
                description = "Toca \"Ver historial\" para ver todos los pagos realizados. Los pagos se registran al marcar como \"Pagado\" desde esta pantalla.",
                targetTag = "recurring_history"
            )
        )

        TutorialModule.HOUSEHOLD -> listOf(
            TutorialStep(
                title = "Tu Hogar",
                description = "Selecciona tu hogar para comenzar. Cada hogar se muestra como una tarjeta visual con gradiente de color único.",
                targetTag = "household_info"
            ),
            TutorialStep(
                title = "Tarjetas Visuales",
                description = "Las tarjetas muestran el nombre, miembros y un ícono. Si tienes varios hogares, aparecen en una cuadrícula de 2 columnas.",
                targetTag = "household_cards"
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

        TutorialModule.WISHLIST -> listOf(
            TutorialStep(
                title = "Lista de Deseos",
                description = "Aquí registras los artículos que el hogar quiere comprar en el futuro.",
                targetTag = "wishlist_list"
            ),
            TutorialStep(
                title = "Agregar deseo",
                description = "Toca + para agregar un nuevo artículo con nombre, costo estimado y prioridad.",
                targetTag = "wishlist_add"
            ),
            TutorialStep(
                title = "Prioridad",
                description = "Clasifica cada artículo como Alta, Media o Baja prioridad para organizar mejor.",
                targetTag = "wishlist_priority"
            ),
            TutorialStep(
                title = "Marcar como comprado",
                description = "Cuando adquieras un artículo, márcalo como comprado desde el menú de opciones.",
                targetTag = "wishlist_purchased"
            )
        )
    }
}
