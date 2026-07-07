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
    val recentLoginRequired: String
    val recentLoginRequiredChangePassword: String
    val passwordUpdated: String
    val ovulationDay: String
    val fertileDay: String
    val registeredActivities: String
    val editPeriodDates: String
    val typeDeleteToConfirm: String
    val newPassword: String
    val ovulationPrediction: String
    val highPregnancyProbability: String
    val lowPregnancyProbability: String
    val symptoms: String
    val sex: String
    val sexAndDesire: String
    val noSex: String
    val protectedSex: String
    val unprotectedSex: String
    val oralSex: String
    val analSex: String
    val masturbation: String
    val sensualContact: String
    val sexToys: String
    val orgasm: String
    val highDesire: String
    val neutralDesire: String
    val lowDesire: String
    val cycleDay: String
    val month: String
    val year: String
    val fertile: String
    val notificationsSection: String
    val batteryOptimization: String
    val batteryOptimizationDesc: String
    val notificationsSettings: String
    val notificationsSettingsDesc: String
    val backgroundData: String
    val backgroundDataDesc: String
    val highPriorityConnection: String
    val highPriorityConnectionDesc: String
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
    override val recentLoginRequired: String,
    override val recentLoginRequiredChangePassword: String,
    override val passwordUpdated: String,
    override val ovulationDay: String,
    override val fertileDay: String,
    override val registeredActivities: String,
    override val editPeriodDates: String,
    override val typeDeleteToConfirm: String,
    override val newPassword: String,
    override val ovulationPrediction: String,
    override val highPregnancyProbability: String,
    override val lowPregnancyProbability: String,
    override val symptoms: String,
    override val sex: String,
    override val sexAndDesire: String,
    override val noSex: String,
    override val protectedSex: String,
    override val unprotectedSex: String,
    override val oralSex: String,
    override val analSex: String,
    override val masturbation: String,
    override val sensualContact: String,
    override val sexToys: String,
    override val orgasm: String,
    override val highDesire: String,
    override val neutralDesire: String,
    override val lowDesire: String,
    override val cycleDay: String,
    override val month: String,
    override val year: String,
    override val fertile: String,
    override val notificationsSection: String,
    override val batteryOptimization: String,
    override val batteryOptimizationDesc: String,
    override val notificationsSettings: String,
    override val notificationsSettingsDesc: String,
    override val backgroundData: String,
    override val backgroundDataDesc: String,
    override val highPriorityConnection: String,
    override val highPriorityConnectionDesc: String
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
        since = "Juntos desde",
        loveExtraLabel = "Amor extra",
        missYou = "Te extraño",
        loveYou = "Te amo",
        years = "Años",
        months = "Meses",
        days = "Días",
        weekDays = "D,L,M,M,J,V,S",
        hoursShort = "h",
        minutesShort = "m",
        secondsShort = "s",
        howDoYouFeel = "¿Cómo se sienten hoy?",
        close = "Cerrar",
        addedBy = "Agregado por",
        someone = "Alguien",
        value = "Valor",
        optional = "Opcional",
        add = "Agregar",
        cancel = "Cancelar",
        details = "Detalles",
        edit = "Editar",
        delete = "Eliminar",
        deleteConfirm = "¿Seguro que quieres eliminar este ítem?",
        clearList = "Limpiar lista",
        clearListConfirm = "¿Seguro que quieres eliminar todos los ítems completados?",
        cleaning = "Aseo",
        product = "Producto",
        category = "Categoría",
        type = "Tipo",
        location = "Lugar",
        selectDate = "Seleccionar fecha",
        back = "Atrás",
        completed = "Completados",
        pending = "Pendientes",
        adventureProgress = "Progreso de aventuras",
        approx = "aprox.",
        charactersLeft = "caracteres restantes",
        loading = "Cargando...",
        answerQuestion = "Responder pregunta",
        answered = "¡Respondido!",
        talkAboutThis = "Hablar sobre esto",
        userActionNotification = "%s ha realizado una acción rápida ✨",
        remindersDesc = "Tareas y recordatorios",
        datesDesc = "Aniversarios y cumpleaños",
        marketDesc = "Lista de compras",
        bucketDesc = "Lista de deseos y planes",
        dailyDesc = "Una pregunta diaria",
        movieTitle = "Título",
        hygiene = "Higiene",
        food = "Comida",
        wishlist = "Favoritos",
        daysLeftShort = "Faltan %d días",
        today = "¡Hoy!",
        tomorrow = "Mañana",
        confirm = "Confirmar",
        yes = "Sí",
        noItemsYet = "Aún no hay ítems",
        noPendingItems = "No hay ítems pendientes",
        restore = "Restaurar",
        addCategory = "Añadir categoría",
        addItem = "Añadir ítem",
        categoryName = "Nombre de la categoría",
        optionalShort = "opc.",
        markedAsWatchedOn = "Marcada como vista el %s",
        productPlaceholder = "Ej: Leche, Huevos...",
        additionalData = "Datos adicionales",
        mapError = "No se pudo abrir el mapa",
        viewOnMap = "Ver en mapa",
        currencyCode = "Código de moneda",
        currencyUsage = "Ej: COP, USD, EUR",
        language = "Idioma",
        exit = "Salir",
        exitConfirm = "¿Estás seguro que deseas salir?",
        welcome = "¡Bienvenido!",
        welcomeBack = "¡Hola de nuevo!",
        visibleCategoriesLabel = "Categorías visibles",
        visibleCategoriesDesc = "Selecciona las categorías que deseas ver en el inicio",
        partner = "Pareja",
        me = "Yo",
        movies = "Cine en casa",
        moviesDesc = "Películas y series por ver",
        series = "Series",
        films = "Películas",
        seen = "Vistas",
        watched = "Visto",
        drawing = "Dibujo",
        drawingDesc = "Lienzo compartido",
        comingSoon = "Próximamente",
        workingOnSection = "Estamos trabajando en esta sección ✨",
        seeAnotherQuestion = "Ver otra pregunta",
        otherWaysToSignIn = "Otras formas de iniciar sesión",
        welcomeToApp = "Bienvenido a Meld",
        email = "Correo electrónico",
        password = "Contraseña",
        confirmPassword = "Confirmar contraseña",
        login = "Iniciar Sesión",
        register = "Registrarse",
        dontHaveAccount = "¿No tienes cuenta? Regístrate",
        alreadyHaveAccount = "¿Ya tienes cuenta? Inicia sesión",
        createAccount = "Crear Cuenta",
        accountCreated = "Cuenta creada exitosamente",
        passwordsDontMatch = "Las contraseñas no coinciden",
        invalidEmailOrPassword = "Email o contraseña inválidos",
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
        recentLoginRequired = "Por seguridad, debes haber iniciado sesión recientemente para eliminar tu cuenta. Por favor, cierra sesión e inicia sesión de nuevo.",
        recentLoginRequiredChangePassword = "Por seguridad, debes haber iniciado sesión recientemente para cambiar tu contraseña. Por favor, cierra sesión e inicia sesión de nuevo.",
        passwordUpdated = "Contraseña actualizada",
        ovulationDay = "Alta probabilidad de quedar embarazada",
        fertileDay = "Probabilidad de quedar embarazada",
        registeredActivities = "Actividades registradas:",
        editPeriodDates = "Editar fechas de periodo",
        typeDeleteToConfirm = "Escribe 'Eliminar' para confirmar",
        newPassword = "Nueva contraseña",
        ovulationPrediction = "Predicción: día de ovulación",
        highPregnancyProbability = "Alta probabilidad de quedarte embarazada",
        lowPregnancyProbability = "Baja probabilidad de quedarte embarazada",
        symptoms = "Síntomas",
        sex = "Sexo",
        sexAndDesire = "Sexo y deseo sexual",
        noSex = "No he practicado sexo",
        protectedSex = "Sexo con protección",
        unprotectedSex = "Sexo sin protección",
        oralSex = "Sexo oral",
        analSex = "Sexo anal",
        masturbation = "Masturbación",
        sensualContact = "Caricias y contacto sensual",
        sexToys = "Juguetes sexuales",
        orgasm = "Orgasmo",
        highDesire = "Deseo sexual elevado",
        neutralDesire = "Deseo sexual neutro",
        lowDesire = "Deseo sexual bajo",
        cycleDay = "Día %d del ciclo",
        month = "Mes",
        year = "Año",
        fertile = "Fértil",
        notificationsSection = "Notificaciones",
        batteryOptimization = "Optimización de Batería",
        batteryOptimizationDesc = "Configura la app como 'Sin restricciones' para recibir notificaciones al instante.",
        notificationsSettings = "Permisos de Notificación",
        notificationsSettingsDesc = "Asegúrate de que todos los canales de notificación estén activados.",
        backgroundData = "Datos en Segundo Plano",
        backgroundDataDesc = "Permite que la app sincronice datos incluso cuando no la estés usando.",
        highPriorityConnection = "Conexión de Alta Prioridad",
        highPriorityConnectionDesc = "Mantiene un escudo de conexión para recibir mensajes incluso con la app cerrada."
    ),
    features = FeatureStrings(
        relation = "Relación",
        relationStatus = "Estado de la relación",
        linkedStatus = "Enlazado con tu pareja",
        relationDetails = "Detalles de la relación",
        viewMembersCode = "Ver código de miembros",
        unlinkPartner = "Desvincular pareja",
        linkWithPartner = "Vincular con pareja",
        generateCode = "Generar código",
        codeCopied = "Código copiado al portapapeles",
        generateAnotherCode = "Generar otro código",
        haveCode = "Ya tengo un código",
        enterPartnerCode = "Ingresa el código de tu pareja",
        members = "Miembros",
        unnamedUser = "Usuario sin nombre",
        linkingCodeLabel = "Tu código de vinculación:",
        noCodeGenerated = "No has generado un código aún",
        shareCodeDesc = "Comparte este código con tu pareja para vincular sus cuentas y compartir listas.",
        linkDialogDesc = "Ingresa el código que generó tu pareja para unirse a su relación.",
        codeMustBe16Digits = "El código debe tener 16 caracteres",
        logout = "Cerrar Sesión",
        customization = "Personalización",
        appearance = "Apariencia",
        content = "Contenido",
        selectedCount = "%d seleccionados",
        modulesCount = "%d módulos",
        admin = "Administrador",
        syncData = "Sincronizar Datos",
        updateFirebase = "Actualizar en la nube",
        synchronizing = "Sincronizando...",
        quickActions = "Acciones Rápidas",
        quickAction1 = "Acción rápida 1",
        quickAction2 = "Acción rápida 2",
        reorderCategories = "Reordenar categorías",
        editModeDesc = "Mantén presionado y arrastra para reordenar",
        dragToReorder = "Arrastrar para reordenar",
        marketList = "Lista de Mercado",
        manageCategories = "Gestionar Categorías",
        newCategory = "Nueva Categoría",
        currency = "Moneda",
        cannotMixItems = "No puedes mezclar ítems de diferentes categorías",
        deleteBought = "Eliminar comprados",
        widgetUpdateWarning = "El widget se actualizará en unos segundos",
        addNew = "Agregar nuevo",
        selectCategory = "Selecciona categoría",
        parentCategory = "Categoría padre",
        name = "Nombre",
        description = "Descripción",
        selectParentError = "Debes seleccionar una categoría padre",
        editAdventure = "Editar aventura",
        deleteAdventure = "Eliminar aventura",
        deleteAdventureDesc = "¿Estás seguro que deseas eliminar esta aventura y todos sus ítems?",
        addToThisCategory = "Añadir a esta categoría",
        currentReminders = "Vigentes",
        notSelected = "No seleccionado",
        dueDateOptional = "Fecha de vencimiento (opcional)",
        massAction = "Acción masiva",
        deleteSelected = "Eliminar seleccionados",
        backgroundColor = "Color de fondo",
        tools = "Herramientas",
        eraser = "Borrador",
        undo = "Deshacer",
        deleteAll = "Borrar todo",
        strokeSize = "Tamaño del trazo",
        sendDrawing = "Enviar dibujo",
        todayDrawings = "Dibujos de hoy",
        noDrawingsToday = "No hay dibujos registrados hoy",
        drawingFrom = "Dibujo de %s",
        sentAt = "Enviado a las %s",
        download = "Descargar",
        editAdd = "Editar / Añadir",
        drawingSaved = "Dibujo guardado en la galería",
        drawingError = "Error al guardar el dibujo",
        drawingSent = "Dibujo enviado ✨",
        quickMessageSent = "Mensaje enviado",
        newDrawingNotification = "¡Tienes un nuevo dibujo de tu pareja! 🎨",
        itemDeleted = "Ítem eliminado",
        skipInitialConfig = "Omitir configuración",
        skipInitialConfigConfirm = "¿Deseas omitir la configuración inicial? Podrás hacerlo más tarde en ajustes.",
        displayMode = "Modo de visualización",
        compactGrid = "Cuadrícula compacta",
        comfortableGrid = "Cuadrícula cómoda",
        coverOnlyGrid = "Solo portadas",
        listMode = "Lista",
        itemsPerRow = "Ítems por fila",
        display = "Visualización",
        deleteCategoryWarning = "¿Deseas eliminar esta categoría y todos sus ítems?",
        deleteMovieCategoryWarning = "Esta es una categoría por defecto y no se puede eliminar",
        episodes = "Episodios",
        seasons = "Temporadas",
        duration = "Duración",
        relationName = "Nombre de la relación",
        renameRelation = "Renombrar relación",
        createProfile = "Crear nuevo perfil",
        selectProfile = "Seleccionar perfil",
        profileName = "Nombre del perfil",
        joinWithCode = "Unirse con código",
        deleteProfile = "Eliminar perfil",
        customizeBackground = "Personalizar fondo",
        color1 = "Color 1",
        color2 = "Color 2",
        backgroundColors = "Cores de fundo",
        intensity = "Intensidad",
        both = "Ambos",
        categoryType = "Tipo de categoría",
        recurrent = "Recurrente",
        resetDay = "Día de reinicio",
        monthly = "Mensual",
        autoReset = "Reinicio automático",
        updateAvailable = "Actualización disponible",
        whatsNew = "¿Qué hay de nuevo?",
        updateInstalled = "Actualización instalada",
        remindersCurrentEmpty = "No tienes recordatorios pendientes ✨",
        remindersRecurrentEmpty = "No hay recordatorios recurrentes configurados",
        importantDatesEmpty = "No hay fechas especiales próximas",
        marketEmpty = "Tu lista de mercado está vacía",
        wishlistEmpty = "Tu lista de deseos está vacía",
        moviesEmpty = "No tienes películas en esta lista",
        bucketEmpty = "Tu lista de aventuras está vacía",
        trackPeriodQuestion = "¿Deseas realizar un seguimiento de tu ciclo menstrual?",
        shareWellnessQuestion = "¿Deseas compartir la información de tu ciclo con tu pareja?",
        advancedConfig = "Configuración Avanzada",
        testNotification = "Probar Notificación"
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
        loveExtraLabel = "Extra love",
        missYou = "Miss you",
        loveYou = "Love you",
        years = "Years",
        months = "Months",
        days = "Days",
        weekDays = "S,M,T,W,T,F,S",
        hoursShort = "h",
        minutesShort = "m",
        secondsShort = "s",
        howDoYouFeel = "How do you feel today?",
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
        deleteConfirm = "Are you sure you want to delete this item?",
        clearList = "Clear list",
        clearListConfirm = "Are you sure you want to delete all completed items?",
        cleaning = "Cleaning",
        product = "Product",
        category = "Category",
        type = "Type",
        location = "Location",
        selectDate = "Select date",
        back = "Back",
        completed = "Completed",
        pending = "Pending",
        adventureProgress = "Adventure progress",
        approx = "approx.",
        charactersLeft = "characters left",
        loading = "Loading...",
        answerQuestion = "Answer question",
        answered = "Answered!",
        talkAboutThis = "Talk about this",
        userActionNotification = "%s has performed a quick action ✨",
        remindersDesc = "Tasks and reminders",
        datesDesc = "Anniversaries and birthdays",
        marketDesc = "Shopping list",
        bucketDesc = "Wishlist and plans",
        dailyDesc = "A daily question",
        movieTitle = "Title",
        hygiene = "Hygiene",
        food = "Food",
        wishlist = "Wishlist",
        daysLeftShort = "%d days left",
        today = "Today!",
        tomorrow = "Tomorrow",
        confirm = "Confirm",
        yes = "Yes",
        noItemsYet = "No items yet",
        noPendingItems = "No pending items",
        restore = "Restore",
        addCategory = "Add category",
        addItem = "Add item",
        categoryName = "Category name",
        optionalShort = "opt.",
        markedAsWatchedOn = "Marked as watched on %s",
        productPlaceholder = "Ex: Milk, Eggs...",
        additionalData = "Additional data",
        mapError = "Could not open map",
        viewOnMap = "View on map",
        currencyCode = "Currency code",
        currencyUsage = "Ex: COP, USD, EUR",
        language = "Language",
        exit = "Exit",
        exitConfirm = "Are you sure you want to exit?",
        welcome = "Welcome!",
        welcomeBack = "Welcome back!",
        visibleCategoriesLabel = "Visible categories",
        visibleCategoriesDesc = "Select categories you want to see on home",
        partner = "Partner",
        me = "Me",
        movies = "Home Cinema",
        moviesDesc = "Movies and series to watch",
        series = "Series",
        films = "Films",
        seen = "Seen",
        watched = "Watched",
        drawing = "Drawing",
        drawingDesc = "Shared canvas",
        comingSoon = "Coming Soon",
        workingOnSection = "We are working on this section ✨",
        seeAnotherQuestion = "See another question",
        otherWaysToSignIn = "Other ways to sign in",
        welcomeToApp = "Welcome to Meld",
        email = "Email",
        password = "Password",
        confirmPassword = "Confirm password",
        login = "Login",
        register = "Register",
        dontHaveAccount = "Don't have an account? Register",
        alreadyHaveAccount = "Already have an account? Login",
        createAccount = "Create Account",
        accountCreated = "Account created successfully",
        passwordsDontMatch = "Passwords don't match",
        invalidEmailOrPassword = "Invalid email or password",
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
        recentLoginRequired = "For security, you must have logged in recently to delete your account. Please log out and log in again.",
        recentLoginRequiredChangePassword = "For security, you must have logged in recently to change your password. Please log out and log in again.",
        passwordUpdated = "Password updated",
        ovulationDay = "High probability of getting pregnant",
        fertileDay = "Probability of getting pregnant",
        registeredActivities = "Registered activities:",
        editPeriodDates = "Edit period dates",
        typeDeleteToConfirm = "Type 'Delete' to confirm",
        newPassword = "New password",
        ovulationPrediction = "Prediction: ovulation day",
        highPregnancyProbability = "High probability of getting pregnant",
        lowPregnancyProbability = "Low probability of getting pregnant",
        symptoms = "Symptoms",
        sex = "Sex",
        sexAndDesire = "Sex and sexual desire",
        noSex = "No sex practiced",
        protectedSex = "Protected sex",
        unprotectedSex = "Unprotected sex",
        oralSex = "Oral sex",
        analSex = "Anal sex",
        masturbation = "Masturbation",
        sensualContact = "Cuddling and sensual contact",
        sexToys = "Sex toys",
        orgasm = "Orgasm",
        highDesire = "High sexual desire",
        neutralDesire = "Neutral sexual desire",
        lowDesire = "Low sexual desire",
        cycleDay = "Cycle day %d",
        month = "Month",
        year = "Year",
        fertile = "Fertile",
        notificationsSection = "Notifications",
        batteryOptimization = "Battery Optimization",
        batteryOptimizationDesc = "Set the app to 'Unrestricted' to receive notifications instantly.",
        notificationsSettings = "Notification Settings",
        notificationsSettingsDesc = "Ensure all notification channels are enabled.",
        backgroundData = "Background Data",
        backgroundDataDesc = "Allow the app to sync data even when you are not using it.",
        highPriorityConnection = "High Priority Connection",
        highPriorityConnectionDesc = "Maintains a connection shield to receive messages even when the app is closed."
    ),
    features = FeatureStrings(
        relation = "Relation",
        relationStatus = "Relation status",
        linkedStatus = "Linked with your partner",
        relationDetails = "Relation details",
        viewMembersCode = "View members code",
        unlinkPartner = "Unlink partner",
        linkWithPartner = "Link with partner",
        generateCode = "Generate code",
        codeCopied = "Code copied to clipboard",
        generateAnotherCode = "Generate another code",
        haveCode = "I already have a code",
        enterPartnerCode = "Enter partner's code",
        members = "Members",
        unnamedUser = "Unnamed user",
        linkingCodeLabel = "Your linking code:",
        noCodeGenerated = "No code generated yet",
        shareCodeDesc = "Share this code with your partner to link accounts and share lists.",
        linkDialogDesc = "Enter the code generated by your partner to join their relation.",
        codeMustBe16Digits = "Code must be 16 characters",
        logout = "Logout",
        customization = "Customization",
        appearance = "Appearance",
        content = "Content",
        selectedCount = "%d selected",
        modulesCount = "%d modules",
        admin = "Admin",
        syncData = "Sync Data",
        updateFirebase = "Update cloud",
        synchronizing = "Synchronizing...",
        quickActions = "Quick Actions",
        quickAction1 = "Quick action 1",
        quickAction2 = "Quick action 2",
        reorderCategories = "Reorder categories",
        editModeDesc = "Long press and drag to reorder",
        dragToReorder = "Drag to reorder",
        marketList = "Shopping List",
        manageCategories = "Manage Categories",
        newCategory = "New Category",
        currency = "Currency",
        cannotMixItems = "Cannot mix items from different categories",
        deleteBought = "Delete bought",
        widgetUpdateWarning = "Widget will update in a few seconds",
        addNew = "Add new",
        selectCategory = "Select category",
        parentCategory = "Parent category",
        name = "Name",
        description = "Description",
        selectParentError = "You must select a parent category",
        editAdventure = "Edit adventure",
        deleteAdventure = "Delete adventure",
        deleteAdventureDesc = "Are you sure you want to delete this adventure and all its items?",
        addToThisCategory = "Add to this category",
        currentReminders = "Current",
        notSelected = "Not selected",
        dueDateOptional = "Due date (optional)",
        massAction = "Mass action",
        deleteSelected = "Delete selected",
        backgroundColor = "Background color",
        tools = "Tools",
        eraser = "Eraser",
        undo = "Undo",
        deleteAll = "Delete all",
        strokeSize = "Stroke size",
        sendDrawing = "Send drawing",
        todayDrawings = "Today's drawings",
        noDrawingsToday = "No drawings registered today",
        drawingFrom = "Drawing from %s",
        sentAt = "Sent at %s",
        download = "Download",
        editAdd = "Edit / Add",
        drawingSaved = "Drawing saved to gallery",
        drawingError = "Error saving drawing",
        drawingSent = "Drawing sent ✨",
        quickMessageSent = "Message sent",
        newDrawingNotification = "You have a new drawing from your partner! 🎨",
        itemDeleted = "Item deleted",
        skipInitialConfig = "Skip setup",
        skipInitialConfigConfirm = "Do you want to skip initial setup? You can do it later in settings.",
        displayMode = "Display mode",
        compactGrid = "Compact grid",
        comfortableGrid = "Comfortable grid",
        coverOnlyGrid = "Covers only",
        listMode = "List",
        itemsPerRow = "Items per row",
        display = "Display",
        deleteCategoryWarning = "Do you want to delete this category and all its items?",
        deleteMovieCategoryWarning = "This is a default category and cannot be deleted",
        episodes = "Episodes",
        seasons = "Seasons",
        duration = "Duration",
        relationName = "Relationship name",
        renameRelation = "Rename relationship",
        createProfile = "Create new profile",
        selectProfile = "Select profile",
        profileName = "Profile name",
        joinWithCode = "Join with code",
        deleteProfile = "Delete profile",
        customizeBackground = "Customize background",
        color1 = "Color 1",
        color2 = "Color 2",
        backgroundColors = "Background colors",
        intensity = "Intensity",
        both = "Both",
        categoryType = "Category type",
        recurrent = "Recurrent",
        resetDay = "Reset day",
        monthly = "Monthly",
        autoReset = "Auto reset",
        updateAvailable = "Update available",
        whatsNew = "What's new?",
        updateInstalled = "Update installed",
        remindersCurrentEmpty = "You have no pending reminders ✨",
        remindersRecurrentEmpty = "No recurrent reminders configured",
        importantDatesEmpty = "No special dates coming up",
        marketEmpty = "Your shopping list is empty",
        wishlistEmpty = "Your wishlist is empty",
        moviesEmpty = "You have no movies in this list",
        bucketEmpty = "Your adventure list is empty",
        trackPeriodQuestion = "Do you want to track your menstrual cycle?",
        shareWellnessQuestion = "Do you want to share cycle info with your partner?",
        advancedConfig = "Advanced Config",
        testNotification = "Test Notification"
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
        since = "Ensemble desde",
        missYou = "Tu me manques",
        loveYou = "Je t'aime",
        years = "A",
        months = "M",
        days = "J",
        weekDays = "D,L,M,M,J,V,S",
        hoursShort = "h",
        minutesShort = "m",
        secondsShort = "s",
        howDoYouFeel = "Comment vous sentez-vous aujourd'hui?",
        close = "Fermer",
        addedBy = "Ajouté par",
        someone = "Quelqu'un",
        value = "Valeur",
        optional = "Optionnel",
        add = "Ajouter",
        cancel = "Annuler",
        details = "Détails",
        edit = "Modifier",
        delete = "Supprimer",
        deleteConfirm = "Êtes-vous sûr de vouloir supprimer cet élément?",
        clearList = "Vider la liste",
        clearListConfirm = "Êtes-vous sûr de vouloir supprimer tous les éléments terminés?",
        cleaning = "Nettoyage",
        product = "Produit",
        category = "Catégorie",
        type = "Type",
        location = "Lieu",
        selectDate = "Choisir une date",
        back = "Retour",
        completed = "Terminés",
        pending = "En attente",
        adventureProgress = "Progression de l'aventure",
        approx = "env.",
        charactersLeft = "caractères restants",
        loading = "Chargement...",
        answerQuestion = "Répondre à la question",
        answered = "Répondu!",
        talkAboutThis = "En parler",
        userActionNotification = "%s a effectué une action rapide ✨",
        remindersDesc = "Tâches et rappels",
        datesDesc = "Anniversaires et fêtes",
        marketDesc = "Liste de courses",
        bucketDesc = "Liste de souhaits et projets",
        dailyDesc = "Une question quotidienne",
        movieTitle = "Titre",
        hygiene = "Hygiène",
        food = "Nourriture",
        wishlist = "Favoris",
        daysLeftShort = "il reste %d jours",
        today = "Aujourd'hui!",
        tomorrow = "Demain",
        confirm = "Confirmer",
        yes = "Oui",
        noItemsYet = "Pas encore d'éléments",
        noPendingItems = "Pas d'éléments en attente",
        restore = "Restaurer",
        addCategory = "Ajouter catégorie",
        addItem = "Ajouter élément",
        categoryName = "Nom de la catégorie",
        optionalShort = "opt.",
        markedAsWatchedOn = "Marqué comme vu le %s",
        productPlaceholder = "Ex: Lait, Œufs...",
        additionalData = "Données additionnelles",
        mapError = "Impossible d'ouvrir la carte",
        viewOnMap = "Voir sur la carte",
        currencyCode = "Code devise",
        currencyUsage = "Ex: COP, USD, EUR",
        language = "Langue",
        exit = "Quitter",
        exitConfirm = "Êtes-vous sûr de vouloir quitter?",
        welcome = "Bienvenue!",
        welcomeBack = "Bon retour!",
        visibleCategoriesLabel = "Catégories visibles",
        visibleCategoriesDesc = "Sélectionnez les catégories à afficher à l'accueil",
        partner = "Partenaire",
        me = "Moi",
        movies = "Ciné à la maison",
        moviesDesc = "Films et séries à voir",
        series = "Séries",
        films = "Films",
        seen = "Vus",
        watched = "Vu",
        drawing = "Dessin",
        drawingDesc = "Canevas partagé",
        comingSoon = "Bientôt disponible",
        workingOnSection = "Nous travaillons sur cette section ✨",
        seeAnotherQuestion = "Voir une autre question",
        otherWaysToSignIn = "Autres moyens de se connecter",
        welcomeToApp = "Bienvenue sur Meld",
        email = "Email",
        password = "Mot de passe",
        confirmPassword = "Confirmer le mot de passe",
        login = "Se Connecter",
        register = "S'inscrire",
        dontHaveAccount = "Pas de compte? S'inscrire",
        alreadyHaveAccount = "Déjà un compte? Se connecter",
        createAccount = "Créer un compte",
        accountCreated = "Compte créé avec succès",
        passwordsDontMatch = "Les mots de passe ne correspondent pas",
        invalidEmailOrPassword = "Email ou mot de passe invalide",
        changeProfilePic = "Changer la photo",
        changeActiveProfile = "Changer le profil actif",
        relationshipDate = "Date de début de relation",
        setDate = "Définir la date",
        mainTitleTooltip = "Définissez un titre pour l'accueil ✨",
        wellness = "Cycle et bien-être",
        wellnessDesc = "Suivi du cycle menstruel",
        periodStatus = "Pendant les règles",
        nextPeriodIn = "Prochaines règles dans",
        noDataRecorded = "Aucune donnée enregistrée",
        logPeriodStart = "Enregistrer le début des règles",
        answeredQuestions = "Questions répondues",
        noQuestionsAnswered = "Vous n'avez pas encore répondu à de questions",
        registeredActivities = "Activités enregistrées:",
        editPeriodDates = "Modifier les dates du cycle",
        newPassword = "Nouveau mot de passe"
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
        massAction = "Action groupée",
        deleteSelected = "Supprimer la sélection"
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
        weekDays = "S,M,D,M,D,F,S",
        hoursShort = "Std.",
        minutesShort = "Min.",
        secondsShort = "Sek.",
        howDoYouFeel = "Wie fühlt ihr euch heute?",
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
        deleteConfirm = "Möchten Sie diesen Artikel wirklich löschen?",
        clearList = "Liste leeren",
        clearListConfirm = "Möchten Sie wirklich alle abgeschlossenen Artikel löschen?",
        cleaning = "Reinigung",
        product = "Produkt",
        category = "Kategorie",
        type = "Typ",
        location = "Ort",
        selectDate = "Datum auswählen",
        back = "Zurück",
        completed = "Abgeschlossen",
        pending = "Ausstehend",
        adventureProgress = "Fortschritt des Abenteuers",
        approx = "ca.",
        charactersLeft = "verbleibende Zeichen",
        loading = "Wird geladen...",
        answerQuestion = "Frage beantworten",
        answered = "Beantwortet!",
        talkAboutThis = "Darüber sprechen",
        userActionNotification = "%s hat eine Schnellaktion ausgeführt ✨",
        remindersDesc = "Aufgaben und Erinnerungen",
        datesDesc = "Jubiläen und Geburtstage",
        marketDesc = "Einkaufsliste",
        bucketDesc = "Wunschliste und Pläne",
        dailyDesc = "Eine tägliche Frage",
        movieTitle = "Titel",
        hygiene = "Hygiene",
        food = "Essen",
        wishlist = "Wunschliste",
        daysLeftShort = "noch %d Tage",
        today = "Heute!",
        tomorrow = "Morgen",
        confirm = "Bestätigen",
        yes = "Ja",
        noItemsYet = "Noch keine Artikel",
        noPendingItems = "Keine ausstehenden Artikel",
        restore = "Wiederherstellen",
        addCategory = "Kategorie hinzufügen",
        addItem = "Artikel hinzufügen",
        categoryName = "Kategoriename",
        optionalShort = "opt.",
        markedAsWatchedOn = "Als gesehen markiert am %s",
        productPlaceholder = "Z.B.: Milch, Eier...",
        additionalData = "Zusätzliche Daten",
        mapError = "Karte konnte nicht geöffnet werden",
        viewOnMap = "Auf Karte ansehen",
        currencyCode = "Währungscode",
        currencyUsage = "Z.B.: COP, USD, EUR",
        language = "Sprache",
        exit = "Beenden",
        exitConfirm = "Sind Sie sicher, dass Sie beenden möchten?",
        welcome = "Willkommen!",
        welcomeBack = "Willkommen zurück!",
        visibleCategoriesLabel = "Sichtbare Kategorien",
        visibleCategoriesDesc = "Wählen Sie Kategorien für die Startseite",
        partner = "Partner",
        me = "Ich",
        movies = "Heimkino",
        moviesDesc = "Filme und Serien zum Ansehen",
        series = "Serien",
        films = "Filme",
        seen = "Gesehen",
        watched = "Gesehen",
        drawing = "Zeichnung",
        drawingDesc = "Geteilte Leinwand",
        comingSoon = "Demnächst",
        workingOnSection = "Wir arbeiten an diesem Bereich ✨",
        seeAnotherQuestion = "Andere Frage ansehen",
        otherWaysToSignIn = "Andere Anmeldemöglichkeiten",
        welcomeToApp = "Willkommen bei Meld",
        email = "E-Mail",
        password = "Passwort",
        confirmPassword = "Passwort bestätigen",
        login = "Anmelden",
        register = "Registrieren",
        dontHaveAccount = "Kein Konto? Registrieren",
        alreadyHaveAccount = "Bereits ein Konto? Anmelden",
        createAccount = "Konto erstellen",
        accountCreated = "Konto erfolgreich erstellt",
        passwordsDontMatch = "Passwörter stimmen nicht überein",
        invalidEmailOrPassword = "Ungültige E-Mail oder Passwort",
        changeProfilePic = "Profilbild ändern",
        changeActiveProfile = "Aktives Profil ändern",
        relationshipDate = "Beziehungsbeginn",
        setDate = "Datum festlegen",
        mainTitleTooltip = "Titel für Startseite festlegen ✨",
        wellness = "Zyklus & Wellness",
        wellnessDesc = "Menstruationszyklus-Tracking",
        periodStatus = "In der Periode",
        nextPeriodIn = "Nächste Periode in",
        noDataRecorded = "Keine Daten aufgezeichnet",
        logPeriodStart = "Periodenbeginn protokollieren",
        answeredQuestions = "Beantwortete Fragen",
        noQuestionsAnswered = "Ihr habt noch keine Fragen beantwortet",
        registeredActivities = "Protokollierte Aktivitäten:",
        editPeriodDates = "Zyklusdaten bearbeiten",
        newPassword = "Neues Passwort"
    ),
    features = EnglishStrings.features.copy(
        relation = "Beziehung",
        unlinkPartner = "Beziehung verlassen",
        logout = "Abmelden",
        undo = "Rückgängig",
        itemDeleted = "Artikel gelöscht",
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
        massAction = "Massenaktion",
        deleteSelected = "Ausgewählte löschen"
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
        weekDays = "D,S,T,Q,Q,S,S",
        hoursShort = "h",
        minutesShort = "m",
        secondsShort = "s",
        howDoYouFeel = "Como vocês se sentem hoje?",
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
        deleteConfirm = "Tem certeza que deseja excluir este item?",
        clearList = "Limpar lista",
        clearListConfirm = "Tem certeza que deseja excluir todos os itens concluídos?",
        cleaning = "Limpeza",
        product = "Produto",
        category = "Categoria",
        type = "Tipo",
        location = "Local",
        selectDate = "Selecionar data",
        back = "Voltar",
        completed = "Concluídos",
        pending = "Pendentes",
        adventureProgress = "Progresso da aventura",
        approx = "aprox.",
        charactersLeft = "caracteres restantes",
        loading = "Carregando...",
        answerQuestion = "Responder pergunta",
        answered = "Respondido!",
        talkAboutThis = "Falar sobre isso",
        userActionNotification = "%s realizou uma ação rápida ✨",
        remindersDesc = "Tarefas e lembretes",
        datesDesc = "Aniversários e festas",
        marketDesc = "Lista de compras",
        bucketDesc = "Lista de desejos e planos",
        dailyDesc = "Uma pergunta diária",
        movieTitle = "Título",
        hygiene = "Higiene",
        food = "Comida",
        wishlist = "Favoritos",
        daysLeftShort = "faltam %d dias",
        today = "Hoje!",
        tomorrow = "Amanhã",
        confirm = "Confirmar",
        yes = "Sim",
        noItemsYet = "Ainda não há itens",
        noPendingItems = "Não há itens pendentes",
        restore = "Restaurar",
        addCategory = "Adicionar categoria",
        addItem = "Adicionar item",
        categoryName = "Nome da categoria",
        optionalShort = "opc.",
        markedAsWatchedOn = "Marcada como vista em %s",
        productPlaceholder = "Ex: Leite, Ovos...",
        additionalData = "Dados adicionais",
        mapError = "Não foi possível abrir o mapa",
        viewOnMap = "Ver no mapa",
        currencyCode = "Código da moeda",
        currencyUsage = "Ex: COP, USD, EUR",
        language = "Idioma",
        exit = "Sair",
        exitConfirm = "Tem certeza que deseja sair?",
        welcome = "Bem-vindo!",
        welcomeBack = "Olá de novo!",
        visibleCategoriesLabel = "Categorias visíveis",
        visibleCategoriesDesc = "Selecione as categorias para exibir no início",
        partner = "Parceiro",
        me = "Eu",
        movies = "Cine em casa",
        moviesDesc = "Filmes e séries para ver",
        series = "Séries",
        films = "Filmes",
        seen = "Vistos",
        watched = "Visto",
        drawing = "Desenho",
        drawingDesc = "Tela compartilhada",
        comingSoon = "Em breve",
        workingOnSection = "Estamos trabalhando nesta seção ✨",
        seeAnotherQuestion = "Ver outra pergunta",
        otherWaysToSignIn = "Outras formas de entrar",
        welcomeToApp = "Bem-vindo ao Meld",
        email = "E-mail",
        password = "Senha",
        confirmPassword = "Confirmar senha",
        login = "Entrar",
        register = "Cadastrar",
        dontHaveAccount = "Não tem conta? Cadastre-se",
        alreadyHaveAccount = "Já tem conta? Entrar",
        createAccount = "Criar Conta",
        accountCreated = "Conta criada com sucesso",
        passwordsDontMatch = "As senhas não coinciden",
        invalidEmailOrPassword = "Email ou senha inválidos",
        changeProfilePic = "Mudar foto de perfil",
        changeActiveProfile = "Mudar perfil ativo",
        relationshipDate = "Data de início do namoro",
        setDate = "Definir data",
        mainTitleTooltip = "Defina um título para ver no início ✨",
        wellness = "Ciclo e bem-estar",
        wellnessDesc = "Acompanhamento do ciclo menstrual",
        periodStatus = "No período",
        nextPeriodIn = "Próximo período em",
        noDataRecorded = "Nenhum dado registrado",
        logPeriodStart = "Registrar início do período",
        answeredQuestions = "Perguntas respondidas",
        noQuestionsAnswered = "Vocês ainda não responderam a nenhuma pergunta",
        registeredActivities = "Atividades registradas:",
        editPeriodDates = "Editar datas do ciclo",
        newPassword = "Nova senha",
        batteryOptimization = "Otimização de Bateria",
        batteryOptimizationDesc = "Configure o app como 'Sem restrições' para recibir notificações instantaneamente.",
        notificationsSettings = "Configurações de Notificação",
        notificationsSettingsDesc = "Certifique-se de que todos os canais de notificación estão ativos.",
        backgroundData = "Dados em Segundo Plano",
        backgroundDataDesc = "Permita que o app sincronize dados mesmo cuando não estiver em uso.",
        highPriorityConnection = "Conexão de Alta Prioridade",
        highPriorityConnectionDesc = "Mantém um escudo de conexão para receber mensagens mesmo com o app fechado."
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
        massAction = "Ação em massa",
        deleteSelected = "Excluir selecionados"
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
