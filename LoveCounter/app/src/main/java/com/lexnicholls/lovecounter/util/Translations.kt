package com.lexnicholls.lovecounter.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

enum class AppLanguage(val code: String, val label: String) {
    System("system", "Sistema"),
    Spanish("es", "Español"),
    English("en", "English"),
    French("fr", "Français"),
    German("de", "Deutsch"),
    Portuguese("pt", "Português")
}

// Dividimos las traducciones en dos interfaces para no exceder el límite de 255 parámetros de JVM/ART
interface IMainStrings {
    val settings: String
    val userName: String
    val writeName: String
    val save: String
    val mainTitle: String
    val apply: String
    val widgetContent: String
    val timer: String
    val reminders: String
    val dates: String
    val dynamicWidget: String
    val dynamicWidgetDesc: String
    val autoRotateInterval: String
    val seconds: String
    val selectAtLeastTwo: String
    val localCurrency: String
    val currencyDesc: String
    val appTheme: String
    val light: String
    val dark: String
    val system: String
    val deviceInfo: String
    val resetId: String
    val idWarning: String
    val start: String
    val market: String
    val bucket: String
    val daily: String
    val story: String
    val since: String
    val loveExtraLabel: String
    val missYou: String
    val loveYou: String
    val years: String
    val months: String
    val days: String
    val weekDays: String
    val hoursShort: String
    val minutesShort: String
    val secondsShort: String
    val howDoYouFeel: String
    val close: String
    val addedBy: String
    val someone: String
    val value: String
    val optional: String
    val add: String
    val cancel: String
    val details: String
    val edit: String
    val delete: String
    val deleteConfirm: String
    val clearList: String
    val clearListConfirm: String
    val cleaning: String
    val product: String
    val category: String
    val type: String
    val location: String
    val selectDate: String
    val back: String
    val completed: String
    val pending: String
    val adventureProgress: String
    val approx: String
    val charactersLeft: String
    val loading: String
    val answerQuestion: String
    val answered: String
    val talkAboutThis: String
    val userActionNotification: String
    val remindersDesc: String
    val datesDesc: String
    val marketDesc: String
    val bucketDesc: String
    val dailyDesc: String
    val movieTitle: String
    val hygiene: String
    val food: String
    val wishlist: String
    val daysLeftShort: String
    val today: String
    val tomorrow: String
    val confirm: String
    val yes: String
    val noItemsYet: String
    val noPendingItems: String
    val restore: String
    val addCategory: String
    val addItem: String
    val categoryName: String
    val optionalShort: String
    val markedAsWatchedOn: String
    val productPlaceholder: String
    val additionalData: String
    val mapError: String
    val viewOnMap: String
    val currencyCode: String
    val currencyUsage: String
    val language: String
    val exit: String
    val exitConfirm: String
    val welcome: String
    val welcomeBack: String
    val visibleCategoriesLabel: String
    val visibleCategoriesDesc: String
    val partner: String
    val me: String
    val movies: String
    val moviesDesc: String
    val series: String
    val films: String
    val seen: String
    val watched: String
    val drawing: String
    val drawingDesc: String
    val comingSoon: String
    val workingOnSection: String
    val seeAnotherQuestion: String
    val otherWaysToSignIn: String
    val welcomeToApp: String
    val email: String
    val password: String
    val confirmPassword: String
    val login: String
    val register: String
    val dontHaveAccount: String
    val alreadyHaveAccount: String
    val createAccount: String
    val accountCreated: String
    val passwordsDontMatch: String
    val invalidEmailOrPassword: String
    val changeProfilePic: String
    val changeActiveProfile: String
    val relationshipDate: String
    val setDate: String
    val mainTitleTooltip: String
    val wellness: String
    val wellnessDesc: String
    val periodStatus: String
    val nextPeriodIn: String
    val noDataRecorded: String
    val logPeriodStart: String
    val answeredQuestions: String
    val noQuestionsAnswered: String
    val changePassword: String
    val deleteAccount: String
    val deleteAccountConfirm: String
    val deleteAccountDesc: String
    val typeDeleteToConfirm: String
    val newPassword: String
}

interface IFeatureStrings {
    val relation: String
    val relationStatus: String
    val linkedStatus: String
    val relationDetails: String
    val viewMembersCode: String
    val unlinkPartner: String
    val linkWithPartner: String
    val generateCode: String
    val codeCopied: String
    val generateAnotherCode: String
    val haveCode: String
    val enterPartnerCode: String
    val members: String
    val unnamedUser: String
    val linkingCodeLabel: String
    val noCodeGenerated: String
    val shareCodeDesc: String
    val linkDialogDesc: String
    val codeMustBe16Digits: String
    val logout: String
    val customization: String
    val appearance: String
    val content: String
    val selectedCount: String
    val modulesCount: String
    val admin: String
    val syncData: String
    val updateFirebase: String
    val synchronizing: String
    val quickActions: String
    val quickAction1: String
    val quickAction2: String
    val reorderCategories: String
    val editModeDesc: String
    val dragToReorder: String
    val marketList: String
    val manageCategories: String
    val newCategory: String
    val currency: String
    val cannotMixItems: String
    val deleteBought: String
    val widgetUpdateWarning: String
    val addNew: String
    val selectCategory: String
    val parentCategory: String
    val name: String
    val description: String
    val selectParentError: String
    val editAdventure: String
    val deleteAdventure: String
    val deleteAdventureDesc: String
    val addToThisCategory: String
    val currentReminders: String
    val notSelected: String
    val dueDateOptional: String
    val massAction: String
    val deleteSelected: String
    val backgroundColor: String
    val tools: String
    val eraser: String
    val undo: String
    val deleteAll: String
    val strokeSize: String
    val sendDrawing: String
    val todayDrawings: String
    val noDrawingsToday: String
    val drawingFrom: String
    val sentAt: String
    val download: String
    val editAdd: String
    val drawingSaved: String
    val drawingError: String
    val drawingSent: String
    val quickMessageSent: String
    val newDrawingNotification: String
    val itemDeleted: String
    val skipInitialConfig: String
    val skipInitialConfigConfirm: String
    val displayMode: String
    val compactGrid: String
    val comfortableGrid: String
    val coverOnlyGrid: String
    val listMode: String
    val itemsPerRow: String
    val display: String
    val deleteCategoryWarning: String
    val deleteMovieCategoryWarning: String
    val episodes: String
    val seasons: String
    val duration: String
    val relationName: String
    val renameRelation: String
    val createProfile: String
    val selectProfile: String
    val profileName: String
    val joinWithCode: String
    val deleteProfile: String
    val customizeBackground: String
    val color1: String
    val color2: String
    val backgroundColors: String
    val intensity: String
    val both: String
    val categoryType: String
    val recurrent: String
    val resetDay: String
    val monthly: String
    val autoReset: String
    val updateAvailable: String
    val whatsNew: String
    val updateInstalled: String
    val remindersCurrentEmpty: String
    val remindersRecurrentEmpty: String
    val importantDatesEmpty: String
    val marketEmpty: String
    val wishlistEmpty: String
    val moviesEmpty: String
    val bucketEmpty: String
    val trackPeriodQuestion: String
    val shareWellnessQuestion: String
    val advancedConfig: String
    val testNotification: String
}

data class MainStrings(
    override val settings: String,
    override val userName: String,
    override val writeName: String,
    override val save: String,
    override val mainTitle: String,
    override val apply: String,
    override val widgetContent: String,
    override val timer: String,
    override val reminders: String,
    override val dates: String,
    override val dynamicWidget: String,
    override val dynamicWidgetDesc: String,
    override val autoRotateInterval: String,
    override val seconds: String,
    override val selectAtLeastTwo: String,
    override val localCurrency: String,
    override val currencyDesc: String,
    override val appTheme: String,
    override val light: String,
    override val dark: String,
    override val system: String,
    override val deviceInfo: String,
    override val resetId: String,
    override val idWarning: String,
    override val start: String,
    override val market: String,
    override val bucket: String,
    override val daily: String,
    override val story: String,
    override val since: String,
    override val loveExtraLabel: String,
    override val missYou: String,
    override val loveYou: String,
    override val years: String,
    override val months: String,
    override val days: String,
    override val weekDays: String,
    override val hoursShort: String,
    override val minutesShort: String,
    override val secondsShort: String,
    override val howDoYouFeel: String,
    override val close: String,
    override val addedBy: String,
    override val someone: String,
    override val value: String,
    override val optional: String,
    override val add: String,
    override val cancel: String,
    override val details: String,
    override val edit: String,
    override val delete: String,
    override val deleteConfirm: String,
    override val clearList: String,
    override val clearListConfirm: String,
    override val cleaning: String,
    override val product: String,
    override val category: String,
    override val type: String,
    override val location: String,
    override val selectDate: String,
    override val back: String,
    override val completed: String,
    override val pending: String,
    override val adventureProgress: String,
    override val approx: String,
    override val charactersLeft: String,
    override val loading: String,
    override val answerQuestion: String,
    override val answered: String,
    override val talkAboutThis: String,
    override val userActionNotification: String,
    override val remindersDesc: String,
    override val datesDesc: String,
    override val marketDesc: String,
    override val bucketDesc: String,
    override val dailyDesc: String,
    override val movieTitle: String,
    override val hygiene: String,
    override val food: String,
    override val wishlist: String,
    override val daysLeftShort: String,
    override val today: String,
    override val tomorrow: String,
    override val confirm: String,
    override val yes: String,
    override val noItemsYet: String,
    override val noPendingItems: String,
    override val restore: String,
    override val addCategory: String,
    override val addItem: String,
    override val categoryName: String,
    override val optionalShort: String,
    override val markedAsWatchedOn: String,
    override val productPlaceholder: String,
    override val additionalData: String,
    override val mapError: String,
    override val viewOnMap: String,
    override val currencyCode: String,
    override val currencyUsage: String,
    override val language: String,
    override val exit: String,
    override val exitConfirm: String,
    override val welcome: String,
    override val welcomeBack: String,
    override val visibleCategoriesLabel: String,
    override val visibleCategoriesDesc: String,
    override val partner: String,
    override val me: String,
    override val movies: String,
    override val moviesDesc: String,
    override val series: String,
    override val films: String,
    override val seen: String,
    override val watched: String,
    override val drawing: String,
    override val drawingDesc: String,
    override val comingSoon: String,
    override val workingOnSection: String,
    override val seeAnotherQuestion: String,
    override val otherWaysToSignIn: String,
    override val welcomeToApp: String,
    override val email: String,
    override val password: String,
    override val confirmPassword: String,
    override val login: String,
    override val register: String,
    override val dontHaveAccount: String,
    override val alreadyHaveAccount: String,
    override val createAccount: String,
    override val accountCreated: String,
    override val passwordsDontMatch: String,
    override val invalidEmailOrPassword: String,
    override val changeProfilePic: String,
    override val changeActiveProfile: String,
    override val relationshipDate: String,
    override val setDate: String,
    override val mainTitleTooltip: String,
    override val wellness: String,
    override val wellnessDesc: String,
    override val periodStatus: String,
    override val nextPeriodIn: String,
    override val noDataRecorded: String,
    override val logPeriodStart: String,
    override val answeredQuestions: String,
    override val noQuestionsAnswered: String,
    override val changePassword: String,
    override val deleteAccount: String,
    override val deleteAccountConfirm: String,
    override val deleteAccountDesc: String,
    override val typeDeleteToConfirm: String,
    override val newPassword: String
) : IMainStrings

data class FeatureStrings(
    override val relation: String,
    override val relationStatus: String,
    override val linkedStatus: String,
    override val relationDetails: String,
    override val viewMembersCode: String,
    override val unlinkPartner: String,
    override val linkWithPartner: String,
    override val generateCode: String,
    override val codeCopied: String,
    override val generateAnotherCode: String,
    override val haveCode: String,
    override val enterPartnerCode: String,
    override val members: String,
    override val unnamedUser: String,
    override val linkingCodeLabel: String,
    override val noCodeGenerated: String,
    override val shareCodeDesc: String,
    override val linkDialogDesc: String,
    override val codeMustBe16Digits: String,
    override val logout: String,
    override val customization: String,
    override val appearance: String,
    override val content: String,
    override val selectedCount: String,
    override val modulesCount: String,
    override val admin: String,
    override val syncData: String,
    override val updateFirebase: String,
    override val synchronizing: String,
    override val quickActions: String,
    override val quickAction1: String,
    override val quickAction2: String,
    override val reorderCategories: String,
    override val editModeDesc: String,
    override val dragToReorder: String,
    override val marketList: String,
    override val manageCategories: String,
    override val newCategory: String,
    override val currency: String,
    override val cannotMixItems: String,
    override val deleteBought: String,
    override val widgetUpdateWarning: String,
    override val addNew: String,
    override val selectCategory: String,
    override val parentCategory: String,
    override val name: String,
    override val description: String,
    override val selectParentError: String,
    override val editAdventure: String,
    override val deleteAdventure: String,
    override val deleteAdventureDesc: String,
    override val addToThisCategory: String,
    override val currentReminders: String,
    override val notSelected: String,
    override val dueDateOptional: String,
    override val massAction: String,
    override val deleteSelected: String,
    override val backgroundColor: String,
    override val tools: String,
    override val eraser: String,
    override val undo: String,
    override val deleteAll: String,
    override val strokeSize: String,
    override val sendDrawing: String,
    override val todayDrawings: String,
    override val noDrawingsToday: String,
    override val drawingFrom: String,
    override val sentAt: String,
    override val download: String,
    override val editAdd: String,
    override val drawingSaved: String,
    override val drawingError: String,
    override val drawingSent: String,
    override val quickMessageSent: String,
    override val newDrawingNotification: String,
    override val itemDeleted: String,
    override val skipInitialConfig: String,
    override val skipInitialConfigConfirm: String,
    override val displayMode: String,
    override val compactGrid: String,
    override val comfortableGrid: String,
    override val coverOnlyGrid: String,
    override val listMode: String,
    override val itemsPerRow: String,
    override val display: String,
    override val deleteCategoryWarning: String,
    override val deleteMovieCategoryWarning: String,
    override val episodes: String,
    override val seasons: String,
    override val duration: String,
    override val relationName: String,
    override val renameRelation: String,
    override val createProfile: String,
    override val selectProfile: String,
    override val profileName: String,
    override val joinWithCode: String,
    override val deleteProfile: String,
    override val customizeBackground: String,
    override val color1: String,
    override val color2: String,
    override val backgroundColors: String,
    override val intensity: String,
    override val both: String,
    override val categoryType: String,
    override val recurrent: String,
    override val resetDay: String,
    override val monthly: String,
    override val autoReset: String,
    override val updateAvailable: String,
    override val whatsNew: String,
    override val updateInstalled: String,
    override val remindersCurrentEmpty: String,
    override val remindersRecurrentEmpty: String,
    override val importantDatesEmpty: String,
    override val marketEmpty: String,
    override val wishlistEmpty: String,
    override val moviesEmpty: String,
    override val bucketEmpty: String,
    override val trackPeriodQuestion: String,
    override val shareWellnessQuestion: String,
    override val advancedConfig: String,
    override val testNotification: String
) : IFeatureStrings

// Clase final que delega a las implementaciones para mantener compatibilidad con strings.propiedad
class Strings(
    val main: MainStrings,
    val features: FeatureStrings
) : IMainStrings by main, IFeatureStrings by features {
    fun copy(
        main: MainStrings = this.main,
        features: FeatureStrings = this.features
    ) = Strings(main, features)
}

val SpanishStrings = Strings(
    main = MainStrings(
        settings = "Configuración",
        userName = "Tu Nombre (para identificarte)",
        writeName = "Escribe tu nombre",
        save = "Guardar",
        mainTitle = "Título de pantalla principal",
        apply = "Aplicar",
        widgetContent = "Contenido del Widget",
        timer = "Contador de días juntos",
        reminders = "Recordatorios",
        dates = "Fechas especiales",
        dynamicWidget = "Widget dinámico",
        dynamicWidgetDesc = "El widget rotará entre las opciones seleccionadas",
        autoRotateInterval = "Intervalo de rotación",
        seconds = "%d segundos",
        selectAtLeastTwo = "Selecciona al menos 2 módulos",
        localCurrency = "Moneda local",
        currencyDesc = "Se usará en la lista de mercado",
        appTheme = "Tema de la aplicación",
        light = "Claro",
        dark = "Oscuro",
        system = "Sistema",
        deviceInfo = "Información del Dispositivo",
        resetId = "Reiniciar ID de dispositivo",
        idWarning = "Si ambos teléfonos tienen el mismo ID, el estado no se actualizará correctamente.",
        start = "Inicio",
        market = "Lista de mercado",
        bucket = "Cosas por hacer",
        daily = "Pregunta del día",
        story = "Nuestra Historia",
        since = "Juntos desde el",
        loveExtraLabel = "Amor",
        missYou = "Te extraño",
        loveYou = "Te amo",
        years = "A",
        months = "M",
        days = "D",
        weekDays = "L,M,M,J,V,S,D",
        hoursShort = "h",
        minutesShort = "m",
        secondsShort = "s",
        howDoYouFeel = "¿Cómo te sientes?",
        close = "Cerrar",
        addedBy = "Añadido por",
        someone = "Alguien",
        value = "Valor",
        optional = "Opcional",
        add = "Añadir",
        cancel = "Cancelar",
        details = "Detalles",
        edit = "Editar",
        delete = "Eliminar",
        deleteConfirm = "¿Estás seguro de que deseas eliminar?",
        clearList = "Limpiar lista",
        clearListConfirm = "¿Deseas eliminar permanentemente todos los elementos ya comprados?",
        cleaning = "Limpiando",
        product = "Producto",
        category = "Categoría",
        type = "Tipo",
        location = "Lugar",
        selectDate = "Selecciona una fecha",
        back = "Volver",
        completed = "Completados",
        pending = "Pendientes",
        adventureProgress = "Progreso de aventuras",
        approx = "Aprox.",
        charactersLeft = "caracteres restantes",
        loading = "Cargando pregunta...",
        answerQuestion = "¡La contestamos! 🔥",
        answered = "¡Contestada!",
        talkAboutThis = "Tómense un momento para conversar sobre esto hoy ❤️",
        userActionNotification = "¡%s ha contestado la pregunta de hoy! 🔥",
        remindersDesc = "Tareas y notas pendientes",
        datesDesc = "Cumpleaños y aniversarios",
        marketDesc = "Lo que nos hace falta",
        bucketDesc = "Nuestras aventuras",
        dailyDesc = "Una pregunta al día",
        hygiene = "Aseo",
        food = "Comida",
        wishlist = "Wishlist",
        daysLeftShort = "faltan %d d",
        today = "Hoy! 🎉",
        tomorrow = "Mañana",
        confirm = "Confirmar",
        yes = "Sí",
        noItemsYet = "Sin ítems aún",
        noPendingItems = "No hay elementos pendientes",
        restore = "Restaurado",
        addCategory = "Añadir categoría",
        addItem = "Añadir nuevo ítem",
        categoryName = "Nombre de la Categoría",
        optionalShort = "Opcional",
        markedAsWatchedOn = "Marcado como visto el %s",
        productPlaceholder = "Nombre del producto",
        additionalData = "Datos adicionales",
        mapError = "No se pudo abrir el mapa",
        viewOnMap = "Ver en mapa",
        currencyCode = "Código",
        currencyUsage = "Se usará en la lista de mercado",
        language = "Idioma",
        exit = "Salir",
        exitConfirm = "¿Deseas salir de la aplicación?",
        welcome = "Bienvenido",
        welcomeBack = "Bienvenido de nuevo",
        visibleCategoriesLabel = "Categorías visibles",
        visibleCategoriesDesc = "Selecciona qué secciones quieres ver en el inicio",
        partner = "Amorcito",
        me = "Yo",
        movies = "Series y películas",
        moviesDesc = "Lo que queremos ver",
        series = "Series",
        films = "Películas",
        seen = "Vista",
        watched = "Visto",
        drawing = "Dibujo libre",
        drawingDesc = "Realiza un dibujo que verá tu relación",
        comingSoon = "Próximamente",
        workingOnSection = "Estamos trabajando en esta sección ✨",
        seeAnotherQuestion = "Ver otra pregunta",
        movieTitle = "Título",
        otherWaysToSignIn = "Otras formas de iniciar sesión",
        welcomeToApp = "¡Bienvenido a la app! ✨",
        email = "Correo electrónico",
        password = "Contraseña",
        confirmPassword = "Confirmar Contraseña",
        login = "Iniciar Sesión",
        register = "Registrarme",
        dontHaveAccount = "¿No tienes cuenta? Regístrate",
        alreadyHaveAccount = "¿Ya tienes cuenta? Inicia sesión",
        createAccount = "Crear Cuenta",
        accountCreated = "Cuenta creada con éxito",
        passwordsDontMatch = "Las contraseñas no coinciden",
        invalidEmailOrPassword = "Correo inválido o contraseña muy corta",
        changeProfilePic = "Cambiar foto de perfil",
        changeActiveProfile = "Cambiar perfil activo",
        relationshipDate = "Fecha de inicio de relación",
        setDate = "Establecer fecha",
        mainTitleTooltip = "Define un título para verlo en el inicio ✨",
        wellness = "Ciclo y bienestar",
        wellnessDesc = "Seguimiento de ciclo menstrual",
        periodStatus = "En periodo",
        nextPeriodIn = "Próximo periodo en",
        noDataRecorded = "No hay datos registrados",
        logPeriodStart = "Registrar Inicio de Periodo",
        answeredQuestions = "Preguntas respondidas",
        noQuestionsAnswered = "Aún no han respondido ninguna pregunta",
        changePassword = "Cambiar contraseña",
        deleteAccount = "Eliminar cuenta",
        deleteAccountConfirm = "¿Confirmas que deseas eliminar tu cuenta?",
        deleteAccountDesc = "Al eliminar tu cuenta se borrará toda la información relacionada (listas y se eliminará de las relaciones en las que apareces).",
        typeDeleteToConfirm = "Escribe 'Eliminar' para confirmar",
        newPassword = "Nueva contraseña"
    ),
    features = FeatureStrings(
        relation = "Relación",
        relationStatus = "Estado de Relación",
        linkedStatus = "¡Enlazado! (%d integrantes)",
        relationDetails = "Detalles de Relación",
        viewMembersCode = "Ver integrantes y código",
        unlinkPartner = "Salir de la relación",
        linkWithPartner = "Vincular con mi pareja",
        generateCode = "Generar código",
        codeCopied = "Código copiado",
        generateAnotherCode = "Generar otro código",
        haveCode = "Tengo un código",
        enterPartnerCode = "Ingresar código de pareja",
        members = "Integrantes:",
        unnamedUser = "Usuario sin nombre",
        linkingCodeLabel = "Código de Enlace:",
        noCodeGenerated = "Sin código generado",
        shareCodeDesc = "Comparte este código para que más personas se unan a esta relación.",
        linkDialogDesc = "Ingresa el código de 16 dígitos generado por tu pareja.",
        codeMustBe16Digits = "El código debe tener 16 dígitos",
        logout = "Cerrar Sesión",
        customization = "Personalización",
        appearance = "Apariencia",
        content = "Contenido",
        selectedCount = "%d seleccionadas",
        modulesCount = "%d módulos",
        admin = "Administración",
        syncData = "Sincronizar Datos",
        updateFirebase = "Actualizar Firebase",
        synchronizing = "Sincronizando...",
        reorderCategories = "Reordenar categorías",
        editModeDesc = "Modo edición: Arrastra las tarjetas para reordenar",
        dragToReorder = "Arrastra las tarjetas para cambiar el orden",
        marketList = "Lista de Mercado",
        manageCategories = "Gestionar categorías de la lista",
        newCategory = "Nueva categoría",
        currency = "Moneda",
        cannotMixItems = "No puedes mezclar items pendientes y comprados",
        deleteBought = "Borrar Comprados",
        widgetUpdateWarning = "Actualiza el widget para ver los cambios✨",
        addNew = "Añadir nuevo",
        selectCategory = "Seleccionar Categoría",
        parentCategory = "Categoría Padre",
        name = "Nombre",
        description = "Descripción",
        selectParentError = "Por favor selecciona una categoría padre",
        editAdventure = "Editar aventura",
        deleteAdventure = "¿Eliminar aventura?",
        deleteAdventureDesc = "Se eliminará '%s' y todas sus tareas.",
        addToThisCategory = "Añadir a esta categoría",
        currentReminders = "Vigentes",
        notSelected = "No seleccionada",
        dueDateOptional = "Fecha límite (Opcional)",
        massAction = "Acción Masiva",
        deleteSelected = "Borrar Seleccionados",
        backgroundColor = "Color de fondo:",
        tools = "Herramientas:",
        eraser = "Borrador",
        undo = "Deshacer",
        deleteAll = "Borrar todo",
        strokeSize = "Tamaño del trazo:",
        sendDrawing = "Enviar dibujo",
        todayDrawings = "Dibujos de hoy",
        noDrawingsToday = "No hay dibujos guardados hoy.",
        drawingFrom = "Dibujo de %s",
        sentAt = "Enviado a las %s",
        itemDeleted = "El ítem se ha eliminado",
        download = "Descargar",
        editAdd = "Editar (Agregar)",
        drawingSaved = "Imagen guardada en Galería",
        drawingError = "Error al guardar imagen",
        drawingSent = "Dibujo enviado ✨",
        quickMessageSent = "Mensaje rápido enviado ✨",
        newDrawingNotification = "¡Hay un nuevo dibujo disponible! 🎨",
        quickActions = "Acciones rápidas (FAB)",
        quickAction1 = "Acción rápida 1",
        quickAction2 = "Acción rápida 2",
        skipInitialConfig = "¿Saltar configuración?",
        skipInitialConfigConfirm = "¿Deseas saltar la configuración inicial? Podrás ajustar todo luego en los ajustes.",
        displayMode = "Modo de visualización",
        compactGrid = "Cuadrícula compacta",
        comfortableGrid = "Cuadrícula cómoda",
        coverOnlyGrid = "Solo portadas",
        listMode = "Lista",
        itemsPerRow = "Ítems por fila",
        display = "Diseño",
        deleteCategoryWarning = "Al ocultar esta categoría, todos los registros asociados a ella se eliminarán permanentemente de la base de datos. ¿Deseas continuar?",
        deleteMovieCategoryWarning = "Al eliminar esta categoria se eliminaran todos los items dentro de ella.",
        episodes = "Episodios",
        seasons = "Temporadas",
        duration = "Duración",
        relationName = "Nombre de la relación",
        renameRelation = "Renombrar relación",
        createProfile = "Crear nuevo perfil",
        selectProfile = "Seleccionar perfil",
        profileName = "Nombre del perfil",
        joinWithCode = "Unirme con código",
        deleteProfile = "Eliminar perfil",
        customizeBackground = "Personalizar Fondo",
        color1 = "Color 1",
        color2 = "Color 2",
        backgroundColors = "Colores de fondo",
        intensity = "Intensidad",
        both = "Ambos",
        categoryType = "Tipo de contenido",
        recurrent = "Recurrentes",
        resetDay = "Día de reinicio",
        monthly = "Mensual",
        autoReset = "Reinicio automático",
        updateAvailable = "Nueva versión disponible",
        whatsNew = "Novedades",
        updateInstalled = "¡Meld actualizado!",
        remindersCurrentEmpty = "Aqui podras llevar control de los recordatorios cercanos como citas medicas o recordatorios a corto plazo.",
        remindersRecurrentEmpty = "Aqui podras llevar control de tus facturas pagadas o pendientes de pago junto con la configuración para que se reestablezcan el dia de tu elección",
        importantDatesEmpty = "Aqui podras agregar fechas especiales lo cual te permitira tener recordatorios de cumpleaños, aniversaior y otras fechas que quieras recordar",
        marketEmpty = "Aqui podras guardar los items que necesites cuando vayas a mercar",
        wishlistEmpty = "Aqui podras agregar compras a futuro o cosas que quieras comprar junto con el precio de ser necesario",
        moviesEmpty = "Aqui podras llevar control de las series y peliculas que quieras ver a futuro",
        bucketEmpty = "Aqui podras agregar cosas por hacer, como restaurantes a visitar, sitios que quieras ver a futuro y guardar el progreso de tus aventuras",
        trackPeriodQuestion = "¿Quieres rastrear tu ciclo menstrual?",
        shareWellnessQuestion = "¿Quieres compartir esta información con tu pareja?",
        advancedConfig = "Configuración avanzada",
        testNotification = "Probar notificación"
    )
)

val EnglishStrings = Strings(
    main = MainStrings(
        settings = "Settings",
        userName = "Your Name (for ID)",
        writeName = "Write your name",
        save = "Save",
        mainTitle = "Main Screen Title",
        apply = "Apply",
        widgetContent = "Widget Content",
        timer = "Days Together Counter",
        reminders = "Reminders",
        dates = "Special Dates",
        dynamicWidget = "Dynamic Widget",
        dynamicWidgetDesc = "The widget will rotate between selected options",
        autoRotateInterval = "Rotation interval",
        seconds = "%d seconds",
        selectAtLeastTwo = "Select at least 2 modules",
        localCurrency = "Local Currency",
        currencyDesc = "Used in the shopping list",
        appTheme = "App Theme",
        light = "Light",
        dark = "Dark",
        system = "System",
        deviceInfo = "Device Info",
        resetId = "Reset Device ID",
        idWarning = "If both phones have the same ID, status won't update correctly.",
        start = "Home",
        market = "Shopping List",
        bucket = "Bucket List",
        daily = "Daily Question",
        story = "Our Story",
        since = "Together since",
        loveExtraLabel = "Love",
        missYou = "I miss you",
        loveYou = "I love you",
        years = "Y",
        months = "M",
        days = "D",
        weekDays = "M,T,W,T,F,S,S",
        hoursShort = "h",
        minutesShort = "m",
        secondsShort = "s",
        howDoYouFeel = "How do you feel?",
        close = "Close",
        addedBy = "Added by",
        someone = "Someone",
        value = "Value",
        optional = "Optional",
        add = "Add",
        cancel = "Cancel",
        details = "Details",
        edit = "Edit",
        delete = "Delete",
        deleteConfirm = "Are you sure you want to delete?",
        clearList = "Clear List",
        clearListConfirm = "Permanently delete all purchased items?",
        cleaning = "Cleaning",
        product = "Product",
        category = "Category",
        type = "Type",
        location = "Location",
        selectDate = "Select a date",
        back = "Back",
        completed = "Completed",
        pending = "Pending",
        adventureProgress = "Adventure Progress",
        approx = "Approx.",
        charactersLeft = "characters left",
        loading = "Loading question...",
        answerQuestion = "Answered! 🔥",
        answered = "Answered!",
        talkAboutThis = "Take a moment to talk about this today ❤️",
        userActionNotification = "%s has answered today's question! 🔥",
        remindersDesc = "Pending tasks and notes",
        datesDesc = "Birthdays and anniversaries",
        marketDesc = "Things we need",
        bucketDesc = "Our adventures",
        dailyDesc = "One question a day",
        hygiene = "Hygiene",
        food = "Food",
        wishlist = "Wishlist",
        daysLeftShort = "%d d left",
        today = "Today! 🎉",
        tomorrow = "Tomorrow",
        confirm = "Confirm",
        yes = "Yes",
        noItemsYet = "No items yet",
        noPendingItems = "No pending items",
        restore = "Restored",
        addCategory = "Add category",
        addItem = "Add new item",
        categoryName = "Category Name",
        optionalShort = "Optional",
        markedAsWatchedOn = "Marked as watched on %s",
        productPlaceholder = "Product name",
        additionalData = "Additional data",
        mapError = "Could not open map",
        viewOnMap = "View on map",
        currencyCode = "Code",
        currencyUsage = "Will be used in shopping list",
        language = "Language",
        exit = "Exit",
        exitConfirm = "Do you want to exit the application?",
        welcome = "Welcome",
        welcomeBack = "Welcome back",
        visibleCategoriesLabel = "Visible Categories",
        visibleCategoriesDesc = "Select which sections you want to see on home",
        partner = "Partner",
        me = "Me",
        movies = "Series & Movies",
        moviesDesc = "What we want to watch",
        series = "Series",
        films = "Movies",
        seen = "Seen",
        watched = "Watched",
        drawing = "Free drawing",
        drawingDesc = "Make a drawing that your partner will see",
        comingSoon = "Coming Soon",
        workingOnSection = "We are working on this section ✨",
        seeAnotherQuestion = "See another question",
        movieTitle = "Title",
        otherWaysToSignIn = "Other ways to sign in",
        welcomeToApp = "Welcome to the app! ✨",
        email = "Email",
        password = "Password",
        confirmPassword = "Confirm Password",
        login = "Login",
        register = "Register",
        dontHaveAccount = "Don't have an account? Register",
        alreadyHaveAccount = "Already have an account? Login",
        createAccount = "Create Account",
        accountCreated = "Account created successfully",
        passwordsDontMatch = "Passwords don't match",
        invalidEmailOrPassword = "Invalid email or short password",
        changeProfilePic = "Change profile picture",
        changeActiveProfile = "Change active profile",
        relationshipDate = "Relationship start date",
        setDate = "Set date",
        mainTitleTooltip = "Set a title to see it on home ✨",
        wellness = "Cycle & Wellness",
        wellnessDesc = "Menstrual cycle tracking",
        periodStatus = "In period",
        nextPeriodIn = "Next period in",
        noDataRecorded = "No data recorded",
        logPeriodStart = "Log Period Start",
        answeredQuestions = "Answered questions",
        noQuestionsAnswered = "You haven't answered any questions yet",
        changePassword = "Change password",
        deleteAccount = "Delete account",
        deleteAccountConfirm = "Confirm you want to delete your account?",
        deleteAccountDesc = "Deleting your account will erase all related information (lists and your presence in relationships).",
        typeDeleteToConfirm = "Type 'Delete' to confirm",
        newPassword = "New password"
    ),
    features = FeatureStrings(
        relation = "Relation",
        relationStatus = "Relationship Status",
        linkedStatus = "Linked! (%d members)",
        relationDetails = "Relationship Details",
        viewMembersCode = "View members and code",
        unlinkPartner = "Leave relationship",
        linkWithPartner = "Link with partner",
        generateCode = "Generate code",
        codeCopied = "Code copied",
        generateAnotherCode = "Generate another code",
        haveCode = "I have a code",
        enterPartnerCode = "Enter partner code",
        members = "Members:",
        unnamedUser = "Unnamed user",
        linkingCodeLabel = "Linking Code:",
        noCodeGenerated = "No code generated",
        shareCodeDesc = "Share this code so more people can join this relationship.",
        linkDialogDesc = "Enter the 16-digit code generated by your partner.",
        codeMustBe16Digits = "The code must be 16 digits",
        logout = "Log Out",
        customization = "Customization",
        appearance = "Appearance",
        content = "Content",
        selectedCount = "%d selected",
        modulesCount = "%d modules",
        admin = "Administration",
        syncData = "Sync Data",
        updateFirebase = "Update Firebase",
        synchronizing = "Synchronizing...",
        reorderCategories = "Reorder categories",
        editModeDesc = "Edit mode: Drag cards to reorder",
        dragToReorder = "Drag cards to change order",
        marketList = "Market List",
        manageCategories = "Manage list categories",
        newCategory = "New category",
        currency = "Currency",
        cannotMixItems = "You cannot mix pending and bought items",
        deleteBought = "Delete Bought",
        widgetUpdateWarning = "Update the widget to see the changes✨",
        addNew = "Add new",
        selectCategory = "Select Category",
        parentCategory = "Parent Category",
        name = "Name",
        description = "Description",
        selectParentError = "Please select a parent category",
        editAdventure = "Edit adventure",
        deleteAdventure = "Delete adventure?",
        deleteAdventureDesc = "'%s' and all its tasks will be deleted.",
        addToThisCategory = "Add to this category",
        currentReminders = "Current",
        notSelected = "Not selected",
        dueDateOptional = "Due Date (Optional)",
        massAction = "Mass Action",
        deleteSelected = "Delete Selected",
        backgroundColor = "Background color:",
        tools = "Tools:",
        eraser = "Eraser",
        undo = "Undo",
        deleteAll = "Delete all",
        strokeSize = "Stroke size:",
        sendDrawing = "Send drawing",
        todayDrawings = "Today's drawings",
        noDrawingsToday = "No drawings saved today.",
        drawingFrom = "Drawing from %s",
        sentAt = "Sent at %s",
        itemDeleted = "Item deleted",
        download = "Download",
        editAdd = "Edit (Add)",
        drawingSaved = "Image saved to Gallery",
        drawingError = "Error saving image",
        drawingSent = "Drawing sent ✨",
        quickMessageSent = "Quick message sent ✨",
        newDrawingNotification = "A new drawing is available! 🎨",
        quickActions = "Quick actions (FAB)",
        quickAction1 = "Quick action 1",
        quickAction2 = "Quick action 2",
        skipInitialConfig = "Skip configuration?",
        skipInitialConfigConfirm = "Do you want to skip the initial configuration? You can adjust everything later in settings.",
        displayMode = "Display mode",
        compactGrid = "Compact grid",
        comfortableGrid = "Comfortable grid",
        coverOnlyGrid = "Cover-only grid",
        listMode = "List",
        itemsPerRow = "Items per row",
        display = "Display",
        deleteCategoryWarning = "By hiding this category, all records associated with it will be permanently deleted from the database. Do you want to continue?",
        deleteMovieCategoryWarning = "By deleting this category, all items within it will be deleted.",
        episodes = "Episodes",
        seasons = "Seasons",
        duration = "Duration",
        relationName = "Relationship Name",
        renameRelation = "Rename Relationship",
        createProfile = "Create New Profile",
        selectProfile = "Select Profile",
        profileName = "Profile Name",
        joinWithCode = "Join with code",
        deleteProfile = "Delete profile",
        customizeBackground = "Customize Background",
        color1 = "Color 1",
        color2 = "Color 2",
        backgroundColors = "Background colors",
        intensity = "Intensity",
        both = "Both",
        categoryType = "Content type",
        recurrent = "Recurrent",
        resetDay = "Reset day",
        monthly = "Monthly",
        autoReset = "Auto-reset",
        updateAvailable = "New version available",
        whatsNew = "What's new",
        updateInstalled = "Meld updated!",
        remindersCurrentEmpty = "Here you can keep track of upcoming reminders like medical appointments or short-term reminders.",
        remindersRecurrentEmpty = "Here you can keep track of your paid or pending bills along with the configuration to reset them on the day of your choice.",
        importantDatesEmpty = "Here you can add special dates which will allow you to have reminders for birthdays, anniversaries, and other dates you want to remember.",
        marketEmpty = "Here you can save the items you need when you go shopping.",
        wishlistEmpty = "Here you can add future purchases or things you want to buy along with the price if necessary.",
        moviesEmpty = "Here you can keep track of the series and movies you want to watch in the future.",
        bucketEmpty = "Here you can add things to do, such as restaurants to visit, places you want to see in the future, and save the progress of your adventures.",
        trackPeriodQuestion = "Do you want to track your menstrual cycle?",
        shareWellnessQuestion = "Do you want to share this information with your partner?",
        advancedConfig = "Advanced settings",
        testNotification = "Test notification"
    )
)

val FrenchStrings = EnglishStrings.copy(
    main = EnglishStrings.main.copy(
        settings = "Paramètres",
        userName = "Votre nom",
        save = "Enregistrer",
        mainTitle = "Titre de l'écran principal",
        apply = "Appliquer",
        widgetContent = "Contenu du widget",
        timer = "Compteur de jours ensemble",
        reminders = "Rappels",
        dates = "Dates spéciales",
        dynamicWidget = "Widget dynamique",
        localCurrency = "Devise locale",
        appTheme = "Thème de l'application",
        light = "Clair",
        dark = "Sombre",
        system = "Système",
        deviceInfo = "Infos sur l'appareil",
        start = "Accueil",
        market = "Liste de courses",
        bucket = "Choses à faire",
        daily = "Question du jour",
        story = "Notre Histoire",
        since = "Ensemble depuis le",
        missYou = "Tu me manques",
        loveYou = "Je t'aime",
        years = "A",
        months = "M",
        days = "J",
        weekDays = "L,M,M,J,V,S,D",
        howDoYouFeel = "Comment te sens-tu ?",
        close = "Fermer",
        addedBy = "Ajouté par",
        someone = "Quelqu'un",
        value = "Valeur",
        optional = "Facultatif",
        add = "Ajouter",
        cancel = "Annuler",
        details = "Détails",
        edit = "Modifier",
        delete = "Supprimer",
        back = "Retour",
        completed = "Terminé",
        charactersLeft = "caracteres restants",
        loading = "Chargement...",
        answerQuestion = "Répondu! 🔥",
        answered = "Répondu!",
        talkAboutThis = "Prenez un moment pour en parler aujourd'hui ❤️",
        userActionNotification = "%s a répondu à la question d'aujourd'hui ! 🔥",
        remindersDesc = "Tâches et notes en attente",
        datesDesc = "Anniversaires et dates spéciales",
        marketDesc = "Ce dont nous avons besoin",
        bucketDesc = "Nos aventures",
        dailyDesc = "Une question par jour",
        hygiene = "Hygiène",
        food = "Nourriture",
        wishlist = "Liste de souhaits",
        daysLeftShort = "encore %d j",
        today = "Aujourd'hui ! 🎉",
        tomorrow = "Demain",
        confirm = "Confirmer",
        yes = "Oui",
        noItemsYet = "Pas encore d'éléments",
        noPendingItems = "Aucun élément en attente",
        restore = "Restauré",
        addCategory = "Ajouter une catégorie",
        addItem = "Ajouter un nouvel élément",
        categoryName = "Nom de la catégorie",
        optionalShort = "Facultatif",
        markedAsWatchedOn = "Marqué comme vu le %s",
        productPlaceholder = "Nom du produit",
        additionalData = "Données supplémentaires",
        mapError = "Impossible d'ouvrir la carte",
        viewOnMap = "Voir sur la carte",
        currencyCode = "Code",
        currencyUsage = "Sera utilisé dans la liste de courses",
        language = "Langue",
        exit = "Quitter",
        exitConfirm = "Voulez-vous quitter l'application ?",
        visibleCategoriesLabel = "Catégories visibles",
        visibleCategoriesDesc = "Sélectionnez les sections à afficher à l'accueil",
        partner = "Amoureux",
        me = "Moi",
        movies = "Séries et films",
        moviesDesc = "Ce qu'on veut regarder",
        series = "Séries",
        films = "Films",
        seen = "Vu",
        watched = "Vu",
        drawing = "Dessin libre",
        drawingDesc = "Faites un dessin que votre partenaire verra",
        movieTitle = "Titre",
        otherWaysToSignIn = "Autres façons de se connecter",
        relationshipDate = "Date de début de relation",
        setDate = "Définir la date",
        answeredQuestions = "Questions répondues",
        noQuestionsAnswered = "Vous n'avez pas encore répondu à de questions"
    ),
    features = EnglishStrings.features.copy(
        relation = "Relation",
        unlinkPartner = "Quitter la relation",
        logout = "Se deconnecter",
        undo = "Annuler",
        itemDeleted = "Élément supprimé",
        reorderCategories = "Réorganiser les catégories",
        marketList = "Liste de courses",
        description = "Description",
        currentReminders = "Actuels",
        strokeSize = "Taille du trait:",
        sendDrawing = "Envoyer le dessin",
        download = "Télécharger",
        customizeBackground = "Personnaliser le fond",
        color1 = "Couleur 1",
        color2 = "Couleur 2",
        backgroundColors = "Couleurs de fond",
        intensity = "Intensité",
        both = "Les deux",
        categoryType = "Type de contenu",
        deleteMovieCategoryWarning = "En supprimant cette catégorie, tous les items à l'intérieur seront supprimés.",
        trackPeriodQuestion = "Voulez-vous suivre votre cycle menstruel ?",
        shareWellnessQuestion = "Voulez-vous partager ces informations avec votre partenaire ?",
        advancedConfig = "Configuration avancée",
        testNotification = "Tester la notification"
    )
)

val GermanStrings = EnglishStrings.copy(
    main = EnglishStrings.main.copy(
        settings = "Einstellungen",
        userName = "Dein Name",
        save = "Speichern",
        mainTitle = "Hauptbildschirm Titel",
        apply = "Anwenden",
        widgetContent = "Widget-Inhalt",
        timer = "Tage zusammen Zähler",
        reminders = "Erinnerungen",
        dates = "Besondere Termine",
        dynamicWidget = "Dynamisches Widget",
        localCurrency = "Lokale Währung",
        appTheme = "App-Design",
        light = "Hell",
        dark = "Dunkel",
        system = "System",
        deviceInfo = "Geräte-Info",
        start = "Start",
        market = "Einkaufsliste",
        bucket = "Bucket-Liste",
        daily = "Frage des Tages",
        story = "Unsere Geschichte",
        since = "Zusammen seit",
        missYou = "Ich vermisse dich",
        loveYou = "Ich liebe dich",
        years = "J",
        months = "M",
        days = "T",
        weekDays = "M,D,M,D,F,S,S",
        howDoYouFeel = "Wie fühlst du dich?",
        close = "Schließen",
        addedBy = "Hinzugefügt von",
        someone = "Jemand",
        value = "Wert",
        optional = "Optional",
        add = "Hinzufügen",
        cancel = "Abbrechen",
        details = "Details",
        edit = "Bearbeiten",
        delete = "Löschen",
        back = "Zurück",
        completed = "Abgeschlossen",
        charactersLeft = "verbleibende Zeichen",
        loading = "Laden...",
        answerQuestion = "Beantwortet! 🔥",
        answered = "Beantwortet!",
        talkAboutThis = "Nehmen Sie sich heute einen Moment Zeit, um darüber zu sprechen ❤️",
        userActionNotification = "%s hat die heutige Frage beantwortet! 🔥",
        remindersDesc = "Anstehende Aufgaben und Notizen",
        datesDesc = "Geburtstage und Jubiläen",
        marketDesc = "Was uns fehlt",
        bucketDesc = "Unsere Abenteuer",
        dailyDesc = "Eine Frage am Tag",
        hygiene = "Hygiene",
        food = "Essen",
        wishlist = "Wunschliste",
        daysLeftShort = "noch %d T",
        today = "Heute! 🎉",
        tomorrow = "Morgen",
        confirm = "Bestätigen",
        yes = "Ja",
        noItemsYet = "Noch keine Elemente",
        noPendingItems = "Keine ausstehenden Elemente",
        restore = "Wiederhergestellt",
        addCategory = "Kategorie hinzufügen",
        addItem = "Neues Element hinzufügen",
        categoryName = "Kategoriename",
        optionalShort = "Optional",
        markedAsWatchedOn = "Markiert als gesehen am %s",
        productPlaceholder = "Produktname",
        additionalData = "Zusätzliche Daten",
        mapError = "Karte konnte nicht geöffnet werden",
        viewOnMap = "Auf Karte anzeigen",
        currencyCode = "Code",
        currencyUsage = "Wird in der Einkaufsliste verwendet",
        language = "Sprache",
        exit = "Beenden",
        exitConfirm = "Möchten Sie die Anwendung verlassen?",
        visibleCategoriesLabel = "Sichtbare Kategorien",
        visibleCategoriesDesc = "Wählen Sie aus, welche Bereiche auf der Startseite angezeigt werden sollen",
        partner = "Partner",
        me = "Ich",
        movies = "Serien & Filme",
        moviesDesc = "Was wir sehen wollen",
        series = "Serien",
        films = "Filme",
        seen = "Gesehen",
        watched = "Gesehen",
        drawing = "Freies Zeichnen",
        drawingDesc = "Erstellen Sie eine Zeichnung, die Ihr Partner sehen wird",
        movieTitle = "Titel",
        otherWaysToSignIn = "Andere Möglichkeiten zur Anmeldung",
        relationshipDate = "Beziehungsbeginn",
        setDate = "Datum festlegen",
        answeredQuestions = "Beantwortete Fragen",
        noQuestionsAnswered = "Du hast noch keine Fragen beantwortet",
        changePassword = "Kennwort ändern",
        deleteAccount = "Konto löschen",
        deleteAccountConfirm = "Bestätigen Sie, dass Sie Ihr Konto löschen möchten?",
        deleteAccountDesc = "Wenn Sie Ihr Konto löschen, werden alle zugehörigen Informationen (Listen und Ihre Anwesenheit in Beziehungen) gelöscht.",
        typeDeleteToConfirm = "Geben Sie 'Löschen' ein, um zu bestätigen",
        newPassword = "Neues Kennwort"
    ),
    features = EnglishStrings.features.copy(
        relation = "Beziehung",
        unlinkPartner = "Beziehung verlassen",
        logout = "Abmelden",
        undo = "Rückgängig",
        itemDeleted = "Element gelöscht",
        reorderCategories = "Kategorien neu ordnen",
        marketList = "Einkaufsliste",
        description = "Beschreibung",
        currentReminders = "Aktuell",
        strokeSize = "Strichstärke:",
        sendDrawing = "Zeichnung senden",
        download = "Herunterladen",
        customizeBackground = "Hintergrund anpassen",
        color1 = "Farbe 1",
        color2 = "Farbe 2",
        backgroundColors = "Hintergrundfarben",
        intensity = "Intensität",
        both = "Beide",
        categoryType = "Inhaltstyp",
        deleteMovieCategoryWarning = "Durch das Löschen dieser Kategorie werden alle darin enthaltenen Elemente gelöscht.",
        trackPeriodQuestion = "Möchten Sie Ihren Menstruationszyklus verfolgen?",
        shareWellnessQuestion = "Möchten Sie diese Informationen mit Ihrem Partner teilen?",
        advancedConfig = "Erweiterte Einstellungen",
        testNotification = "Test-Benachrichtigung"
    )
)

val PortugueseStrings = EnglishStrings.copy(
    main = EnglishStrings.main.copy(
        settings = "Configurações",
        userName = "Seu Nome",
        save = "Salvar",
        mainTitle = "Título da Tela Principal",
        apply = "Aplicar",
        widgetContent = "Conteúdo do Widget",
        timer = "Contador de dias juntos",
        reminders = "Lembretes",
        dates = "Datas Especiais",
        dynamicWidget = "Widget dinámico",
        localCurrency = "Moeda local",
        appTheme = "Tema do aplicativo",
        light = "Claro",
        dark = "Escuro",
        system = "Sistema",
        deviceInfo = "Informações do Dispositivo",
        start = "Início",
        market = "Lista de mercado",
        bucket = "Coisas para fazer",
        daily = "Pergunta do dia",
        story = "Nossa História",
        since = "Juntos desde",
        missYou = "Sinto sua falta",
        loveYou = "Te amo",
        years = "A",
        months = "M",
        days = "D",
        weekDays = "S,T,Q,Q,S,S,D",
        howDoYouFeel = "Como você se sente?",
        close = "Fechar",
        addedBy = "Adicionado por",
        someone = "Alguém",
        value = "Valor",
        optional = "Opcional",
        add = "Adicionar",
        cancel = "Cancelar",
        details = "Detalhes",
        edit = "Editar",
        delete = "Excluir",
        back = "Voltar",
        completed = "Concluído",
        charactersLeft = "caracteres restantes",
        loading = "Carregando...",
        answerQuestion = "Respondido! 🔥",
        answered = "Respondido!",
        talkAboutThis = "Reserve um momento para conversar sobre isso hoje ❤️",
        userActionNotification = "%s respondeu à pergunta de hoje! 🔥",
        remindersDesc = "Tarefas e notas pendentes",
        datesDesc = "Aniversários e datas especiais",
        marketDesc = "O que nos faz falta",
        bucketDesc = "Nossas aventuras",
        dailyDesc = "Uma pergunta por dia",
        hygiene = "Higiene",
        food = "Comida",
        wishlist = "Lista de desejos",
        daysLeftShort = "faltam %d d",
        today = "Hoje! 🎉",
        tomorrow = "Amanhã",
        confirm = "Confirmar",
        yes = "Sim",
        noItemsYet = "Sem itens ainda",
        noPendingItems = "Não há itens pendentes",
        restore = "Restaurado",
        addCategory = "Adicionar categoria",
        addItem = "Adicionar novo item",
        categoryName = "Nome da Categoria",
        optionalShort = "Opcional",
        markedAsWatchedOn = "Marcado como visto em %s",
        productPlaceholder = "Nome do produto",
        additionalData = "Dados adicionais",
        mapError = "Não foi possível abrir o mapa",
        viewOnMap = "Ver no mapa",
        currencyCode = "Código",
        currencyUsage = "Será usado na lista de mercado",
        language = "Idioma",
        exit = "Sair",
        exitConfirm = "Deseja sair do aplicativo?",
        visibleCategoriesLabel = "Categorias visíveis",
        visibleCategoriesDesc = "Selecione quais seções deseja ver no início",
        partner = "Amorzinho",
        me = "Eu",
        movies = "Séries e filmes",
        moviesDesc = "O que queremos ver",
        series = "Séries",
        films = "Filmes",
        seen = "Vista",
        watched = "Assistido",
        drawing = "Desenho livre",
        drawingDesc = "Faça um desenho que seu parceiro verá",
        movieTitle = "Título",
        otherWaysToSignIn = "Outras formas de entrar",
        relationshipDate = "Data de início do relacionamento",
        setDate = "Definir data",
        answeredQuestions = "Perguntas respondidas",
        noQuestionsAnswered = "Você ainda não respondeu a nenhuma pergunta",
        changePassword = "Alterar senha",
        deleteAccount = "Excluir conta",
        deleteAccountConfirm = "Confirma que deseja excluir sua conta?",
        deleteAccountDesc = "A exclusão da sua conta apagará todas as informações relacionadas (listas e sua presença em relacionamentos).",
        typeDeleteToConfirm = "Digite 'Excluir' para confirmar",
        newPassword = "Nova senha"
    ),
    features = EnglishStrings.features.copy(
        relation = "Relação",
        unlinkPartner = "Sair da relação",
        logout = "Sair",
        undo = "Desfazer",
        itemDeleted = "Item excluído",
        reorderCategories = "Reordenar categorias",
        marketList = "Lista de Mercado",
        description = "Descrição",
        currentReminders = "Vigentes",
        strokeSize = "Tamanho do traço:",
        sendDrawing = "Enviar desenho",
        download = "Baixar",
        customizeBackground = "Personalizar fundo",
        color1 = "Cor 1",
        color2 = "Cor 2",
        backgroundColors = "Cores de fundo",
        intensity = "Intensidade",
        both = "Ambos",
        categoryType = "Tipo de conteúdo",
        deleteMovieCategoryWarning = "Ao excluir esta categoria, todos os itens dentro dela serão excluídos.",
        trackPeriodQuestion = "Você quer rastrear seu ciclo menstrual?",
        shareWellnessQuestion = "Você quer compartilhar esta informação com seu parceiro?",
        advancedConfig = "Configurações avançadas",
        testNotification = "Testar notificação"
    )
)

val LocalStrings = staticCompositionLocalOf { SpanishStrings }

fun getStringsForLanguage(languageCode: String): Strings {
    return when (languageCode) {
        "es" -> SpanishStrings
        "en" -> EnglishStrings
        "fr" -> FrenchStrings
        "de" -> GermanStrings
        "pt" -> PortugueseStrings
        else -> EnglishStrings
    }
}

@Composable
fun ProvideStrings(languageCode: String, content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val strings = when (languageCode) {
        "es" -> SpanishStrings
        "en" -> EnglishStrings
        "fr" -> FrenchStrings
        "de" -> GermanStrings
        "pt" -> PortugueseStrings
        else -> {
            val systemLocale = configuration.locales[0].language
            when (systemLocale) {
                "es" -> SpanishStrings
                "en" -> EnglishStrings
                "fr" -> FrenchStrings
                "de" -> GermanStrings
                "pt" -> PortugueseStrings
                else -> EnglishStrings
            }
        }
    }
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}

@Composable
@ReadOnlyComposable
fun t(): Strings = LocalStrings.current
