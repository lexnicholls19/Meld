package com.lexnicholls.lovecounter.data.repository

import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object QuestionsRepository {
    private val _privateQuestions = mutableStateOf(listOf(
        """[😂] Si Rocco de repente pudiera hablar por 24 horas, ¿qué crees que sería lo primero que diría sobre nuestra dinámica en casa?""",
        """[🤔] ¿Cuál es ese pequeño detalle de nuestra rutina diaria que más disfrutas pero que rara vez mencionas?""",
        """[🥵] ¿Cuál ha sido el lugar más inusual o el momento más inesperado donde has fantaseado conmigo?""",
        """[💭] ¿De qué manera sientes que hemos crecido más como equipo en el último año?""",
        """[😂] Si nuestra vida fuera una campaña de D&D, ¿cuáles serían nuestras clases y quién sería el verdadero jefe final de nuestro día a día?""",
        """[🤔] Si tuvieras que comer solo una receta de las que preparamos en la freidora de aire o en la olla a presión por el resto del año, ¿cuál elegirías sin dudarlo?""",
        """[🥵] ¿Qué prenda de ropa mía (incluso la de andar por casa) te resulta secretamente irresistible?""",
        """[💭] ¿Cuál es el recuerdo caminando o pasando el rato por Mosquera que guardas con más cariño?""",
        """[😂] Si tuviéramos que sobrevivir a un apocalipsis zombie usando solo los aparatos tecnológicos, teléfonos y tablets que tenemos en casa, ¿cuál sería nuestra estrategia?""",
        """[🤔] ¿Qué habilidad mía, por más inútil o específica que parezca, te sorprende o te da un poco de envidia?""",
        """[🥵] Si esta noche fuera nuestra primera cita de nuevo, pero con la confianza que ya nos tenemos, ¿cómo intentarías seducirme?""",
        """[💭] ¿Qué aspecto de nuestra comunicación crees que ha mejorado más desde que empezamos a vivir juntos?""",
        """[😂] Jugando cooperativos como Lethal Company, ¿cuál dirías que es mi peor hábito o la forma más tonta en la que suelo hacernos perder?""",
        """[🤔] ¿Qué canción o género musical te recordará siempre a nuestros primeros meses de conocernos?""",
        """[🥵] ¿Qué tipo de caricias, masajes o contacto físico te relaja más después de un día lleno de estrés?""",
        """[💭] ¿Cómo te imaginas una noche de viernes perfecta para nosotros de aquí a 10 años?""",
        """[😂] Si tuviéramos un programa de cocina juntos de batch cooking, ¿cuál sería nuestro platillo estrella y quién sería el chef gruñón?""",
        """[🤔] ¿Cuál ha sido tu momento de mayor orgullo viéndome lograr algo (ya sea en el trabajo, programando o armando algún equipo complejo)?""",
        """[🥵] ¿Qué idea atrevida o juego nuevo te gustaría que exploráramos en los próximos meses?""",
        """[💭] ¿Qué es algo específico que hago por ti que te hace sentir verdaderamente amado/a y protegido/a?""",
        """[😂] Si nuestro Pomeranian nos pusiera una calificación del 1 al 10 como cuidadores, ¿en qué punto exacto nos bajaría puntos?""",
        """[🤔] ¿Qué tradición de pareja que hemos creado de la nada es tu favorita absoluta?""",
        """[🥵] En en momento del día o situación cotidiana te sorprendes sintiéndote más atraído/a hacia mí?""",
        """[💭] ¿Qué lección importante sobre el amor o la convivencia has aprendido a mi lado?""",
        """[😂] Si tuviéramos que describirme usando solo nombres de aplicaciones, programas o lenguajes de código, ¿cuáles elegirías?""",
        """[🤔] ¿Hay alguna meta, viaje o proyecto loco que te gustaría que persiguiéramos juntos, sin importar el presupuesto ni la lógica?""",
        """[🥵] ¿Qué mensaje de texto te he enviado en medio del día que te hizo sonreír pícaramente y que aún recuerdas?""",
        """[💭] En los momentos en los que me frustro con un proyecto, ¿cuál es la mejor manera en la que sientes que puedes apoyarme?""",
        """[😂] Si tuviéramos que elegir a uno de nosotros para negociar nuestra salvación con extraterrestres, ¿un quién iría y qué les ofrecería?""",
        """[💭] De todas nuestras "primeras veces" (el primer viaje, la primera cena que preparamos, la primera mascota), ¿cuál atesoras más?""",
        """[😂] Si estuviera programando una app móvil exclusivamente para nosotros dos, ¿qué función secreta o botón de emergencia tendría que incluir sí o sí?""",
        """[🤔] ¿Qué es algo que pensabas sobre mí antes de conocerme bien y que resultó ser totalmente falso?""",
        """[🥵] Físicamente hablando, ¿cuál es mi mejor ángulo según tú?""",
        """[💭] ¿Cuándo fue la última vez que te sentiste profundamente orgulloso/a de la forma en que manejamos un problema juntos?""",
        """[😂] Si Rocco tuviera un superpoder basado en su personalidad tan particular, ¿cuál sería y cómo lo usaría contra nosotros?""",
        """[🤔] De todos los regalos que nos hemos dado, ¿cuál es el que te ha tomado más por sorpresa?""",
        """[🥵] Si estuviéramos atrapados en un ascensor del centro comercial a altas horas de la noche, ¿qué terminaríamos haciendo para "pasar el rato"?""",
        """[💭] ¿Qué límite o regla invisible tenemos en nuestra relación que crees que es la clave de nuestra paz mental?""",
        """[😂] Si yo fuera una pieza de hardware de un PC que estás armando desde cero, ¿qué componente sería y por qué?""",
        """[🤔] ¿Qué escena de película o serie de televisión has visto y pensado de inmediato: "esos somos literalmente nosotros"?""",
        """[🥵] ¿Qué palabra o frase dicha al oído en el momento adecuado te enciende automáticamente?""",
        """[💭] ¿Qué miedo o inseguridad tenías sobre formalizar una relación que ha desaparecido por completo a mi lado?""",
        """[😂] En una situación de emergencia real, ¿quién de los dos sería el estratega frío y quién el que se lanza a la acción impulsivamente?""",
        """[🤔] ¿Cuál crees que es mi mayor talento oculto que no aprovecho lo suficiente en mi día a día?""",
        """[🥵] Si pudieras elegir mi atuendo completo para una cena en casa totalmente a solas, ¿qué me harías poner?""",
        """[💭] ¿Qué hábito mío (bueno o curioso) te ha inspirado a cambiar algo en ti o a intentar algo nuevo?""",
        """[😂] Si tuviéramos que elegir entre comer solo pizza o solo pollo estilo colombiano por un mes entero, ¿cuál ganaríamos el debate?""",
        """[🤔] ¿Qué anécdota nuestra, por más vergüenza que nos dio en su momento, siempre te hace reír a carcajadas hoy en día?""",
        """[🥵] ¿Alguna vez has tenido un sueño subido de tono conmigo, de esos que te despiertan agitado/a, y que nunca me confesaste?""",
        """[💭] En en qué aspecto de la personalidad sientes que somos polos opuestos pero nos complementamos a la perfección?""",
        """[😂] ¿Qué videojuego es el que más ha puesto a prueba nuestra paciencia mutua sin llegar a pelear de verdad?""",
        """[🤔] ¿Qué pequeño rincón, objeto o detalle de nuestra casa en Mosquera sientes que tiene más de nuestra "esencia"?""",
        """[🥵] ¿Qué tipo de beso es el que más disfrutas que te dé cuando estás distraído/a haciendo otra cosa?""",
        """[💭] ¿Cuál consideras que es el valor humano más fundamental que compartimos y que hace que lo nuestro funcione?""",
        """[😂] Si pudieras reescribir la historia de cómo nos conocimos para que sonara como una novela épica de fantasía, ¿cómo iniciaría el primer capítulo?""",
        """[🤔] ¿De qué tema complejo o raro podríamos quedarnos hablando hasta las 3 de la mañana sin aburrirnos nunca?""",
        """[🥵] Si tuviéramos un fin de semana entero sin responsabilidades, mascotas ni celulares, ¿cómo empezaríamos la mañana del sábado?""",
        """[💭] ¿Qué es lo que más te conmovía de la forma en que nos cuidamos el uno al otro cuando estamos enfermos o cansados?""",
        """[😂] Si nuestro estilo de vida fuera una marca, ¿cuál sería nuestro eslogan oficial?""",
        """[💭] ¿Cuál es la promesa más importante que sientes que nos hemos hecho mutuamente, incluso si nunca la dijimos en voz alta?""",
        """[😂] Si Rolan Deskar cobrara vida y tuviera que darnos consejos de pareja basados en sus aventuras de D&D, ¿qué locura nos recomendaría hacer para resolver nuestras diferencias?""",
        """[🤔] ¿Qué pequeño detalle de nuestras tardes de café con Juan Valdez o en alguna panadería te hace sentir que el día ya valió la pena?""",
        """[🥵] ¿Cuál es ese roce casual o gesto físico "inocente" que hago en público y que te deja pensando en llegar rápido a casa?""",
        """[💭] ¿Cuál crees que ha sido el mayor obstáculo que hemos superado juntos este último año y cómo nos ha transformado?""",
        """[😂] Si nuestra vida fuera un juego de Nintendo, ¿cuál de los dos sería el héroe principal y cuál el compañero que siempre necesita que lo rescaten?""",
        """[🤔] Cuando estoy profundamente concentrado programando o cambiando componentes del PC, ¿qué piensas realmente al verme tan absorto en mi mundo?""",
        """[🥵] ¿Alguna vez me has mirado de lejos en una reunión familiar o con amigos y has tenido un pensamiento totalmente inapropiado?""",
        """[💭] ¿En qué momento exacto te diste cuenta de que Laura y Alexander éramos un equipo definitivo?""",
        """[😂] Si tuviéramos que participar en un reality show de cocinar salchipapa costeña o carne para 16 personas bajo presión, ¿terminaríamos triunfando o peleando por la receta?""",
        """[🤔] ¿Qué rasgo de tu personalidad crees que yo he adoptado desde que estamos juntos?""",
        """[🥵] Si de repente se cortara la luz en toda la zona y no tuviéramos internet ni pantallas en toda la noche, ¿cuál sería tu plan para aprovechar la oscuridad?""",
        """[💭] ¿Qué tradición o costumbre de mi familia (como de mis padres o mi hermana Natalia) te ha sorprendido o te ha gustado más desde que nos conocemos?""",
        """[😂] Si nuestro querido Rocco fuera el juez imparcial de quién de los dos es el más consentidor, ¿un quién ganaríamos el premio y por qué trampa?""",
        """[🤔] ¿Cuál ha sido el viaje o salida más improvisada que hemos hecho y que resultó ser increíble de principio a fin?""",
        """[🥵] ¿Qué tipo de lencería, ropa interior o atuendo de dormir te gustaría verme usar más seguido?""",
        """[💭] ¿Qué sueño personal mío sientes que con el tiempo se ha convertido en un sueño tuyo también?""",
        """[😂] Jugando títulos de terror o tensión como Dead Space, ¿quién de los dos es el que más se asusta y quién disimula peor el miedo?""",
        """[🤔] Si pudieras borrar un solo recuerdo vergonzoso de nuestra relación para vivirlo desde cero y sin cometer errores, ¿cuál sería?""",
        """[🥵] ¿Cuál es tu forma favorita en la que tomo la iniciativa cuando estamos por fin a solas en la cama?""",
        """[💭] ¿De qué manera crees que el proyecto de crear la app lovecounter representa lo que intentamos construir el uno con el otro?""",
        """[😂] Si tuviéramos que elegir entre comer solo pizza o solo pollo estilo colombiano por un mes entero, ¿cuál ganaríamos el debate?""",
        """[🤔] ¿Qué talento o habilidad mía crees que debería intentar monetizar o mostrarle más al mundo?""",
        """[🥵] ¿Hay alguna fantasía o escenario que te dé pena confesarme pero que te encantaría intentar al menos una vez?""",
        """[💭] Cuando me ves interactuar con Rocco preparándole con tanto cuidado sus porciones especiales, ¿qué piensas sobre cómo somos cuidando a los que amamos?""",
        """[😂] Si tuviéramos que huir de Mosquera y mudarnos al otro lado del mundo en menos de una hora con una sola maleta cada uno, ¿qué empacaríamos primero?""",
        """[🤔] ¿Qué olor específico de nuestra casa o de mi ropa te transmite una sensación instantánea de paz y hogar?""",
        """[🥵] ¿En qué lugar específico de la casa, aparte de la habitación, te gustaría que tuviéramos un encuentro apasionado esta misma semana?""",
        """[💭] ¿Qué cualidad mía crees que me ayuda más a superar mis propios límites profesionales o personales?""",
        """[😂] Si nos convirtiéramos en un dúo de supervillanos, ¿un quién inventaría los planes malvados y un quién se encargaría de toda la logística tecnológica?""",
        """[💭] ¿Cuál es esa charla nocturna que hemos tenido, quizá de madrugada, que te ha hecho sentir más cerca de mi alma?""",
        """[😂] Si hiciéramos un manual de instrucciones sobre cómo lidiar con Alexander Nicholls cuando tiene mucha hambre o mucho sueño, ¿cuál sería la regla número uno en negrita?""",
        """[🤔] ¿Qué canción de las que solemos escuchar consideras que es nuestra banda sonora oficial en esta etapa de la vida?""",
        """[🥵] ¿Qué detalle sutil de mi comportamiento te hace saber inmediatamente que estoy con ganas de ti, incluso antes de que pronuncie una palabra?""",
        """[💭] ¿De qué forma sientes que nuestra relación te ha ayudado a sanar cosas de tu pasado?""",
        """[😂] Si por alguna extraña ley nos obligaran a vestir con atuendos combinados todos los días, ¿qué estilo elegiríamos para no hacer el ridículo?""",
        """[🤔] ¿Qué pregunta sobre mí siempre has tenido en la cabeza pero nunca te has atrevido a formular por curiosidad a mi respuesta?""",
        """[🥵] ¿Cuál de mis sentidos (vista, tacto, oído, gusto, olfato) crees que es el que más disfrutas estimular cuando estamos en la intimidad?""",
        """[💭] ¿Qué es lo que más te valoras de la paciencia que nos tenemos cuando la tecnología nos falla, los proyectos no compilan o los días son pesados?""",
        """[😂] Si yo fuera un personaje del universo de The Legend of Zelda, ¿qué tipo de NPC o compañero de viaje sería y qué objeto te vendería?""",
        """[🤔] ¿Qué anécdota o travesura de tu infancia te gustaría que yo hubiera podido presenciar en vivo y en directo?""",
        """[🥵] ¿Alguna vez has utilizado un recuerdo de nosotros en un momento apasionado para motivarte, distraerte o relajarte durante un día muy estresante?""",
        """[💭] ¿Cuál es tu forma favorita de recibir apoyo cuando te sientes abrumado/a por tus propias responsabilidades?""",
        """[😂] Si organizáramos otro evento grande para invitados en casa, ¿cuál sería el desastre más divertido o probable en la cocina si no nos pusiéramos de acuerdo?""",
        """[🤔] ¿Qué pequeño gesto o detalle que hago casi en automático te hace pensar en silencio: "sí, definitivamente elegí bien"?""",
        """[🥵] Si pudieras pedirme que te hiciera un masaje en una sola zona del cuerpo durante media hora entera, ¿donde sería y cómo me lo pedirías?""",
        """[💭] ¿Qué cualidad de Laura admira más Alexander en secreto, y qué cualidad mía admiras tú más que nadie?""",
        """[😂] Si nuestros celulares o tablets pudieran hablar, ¿de qué extraña costumbre nuestra frente a la pantalla se quejarían primero?""",
        """[🤔] ¿Cuál ha sido el cambio físico, de actitud o de estilo en mí a lo largo del tiempo que más te ha cautivado?""",
        """[🥵] ¿Te gusta más que el preámbulo a la intimidad sea lento, lleno de tensión y romántico, o algo mucho más directo, intenso y salvaje?""",
        """[💭] ¿Cómo te imaginas el interior de nuestro hogar ideal si el dinero, el tiempo y el espacio no fueran un problema?""",
        """[😂] Si Rocco escribiera un libro de memorias sobre sus 13 años de vida y su opinión sobre nosotros, ¿cuál sería el título del best-seller?""",
        """[🤔] ¿Qué hobby o actividad que actualmente no compartimos te llama la atención en secreto y te gustaría que intentáramos juntos?""",
        """[🥵] ¿Cuál es tu recuerdo más nítido o la sensación que más recuerdas de la primera vez que estuvimos a solas?""",
        """[💭] ¿Qué crees que es lo que hace que la forma en que nos amamos sea diferente a la de cualquier otra pareja que conocemos?""",
        """[😂] Si un virus informático bloqueara mi ecosistema de dispositivos y la única forma de recuperarlos fuera que resolvieras una adivinanza sobre mí, ¿cuál sería?""",
        """[🤔] ¿Qué película o tema complejo te dejó pensando tanto la última vez que te hubiera gustado quedarte debatiendo sus teorías conmigo toda la noche?""",
        """[🥵] ¿A qué parte de mi cuerpo sientes que le prestas mucha atención, pero yo rara vez me doy cuenta de que la estás mirando?""",
        """[💭] ¿Qué consejo sobre el amor y la paciencia te darías a ti mismo/a si pudieras viajar en el tiempo al día exacto antes de conocernos?""",
        """[😂] Si tuviéramos que crear un diccionario exclusivo de nuestra relación con palabras inventadas que solo nosotros entendemos, ¿cuál sería la primera palabra y su definición?""",
        """[💭] Al mirar hacia el futuro juntos y todo lo que nos falta por construir, ¿qué te genera más emoción y qué te da un poco de vértigo positivo?""",
        """[😂] Si Elisia y Rolan Deskar tuvieran una cita romántica en nuestro mundo en lugar de estar luchando en una campaña, ¿a dónde irían y quién terminaría causando un desastre primero?""",
        """[🤔] ¿Qué pequeño hábito o manía tienes al dormir que crees que yo aún no he descubierto o no te he mencionado?""",
        """[🥵] ¿Cuál es ese elogio o piropo sobre tu cuerpo que siempre te ha gustado escuchar de mis labios?""",
        """[💭] ¿Cómo crees que ha evolucionado tu concepto del amor desde que empezamos a construir nuestra vida juntos?""",
        """[😂] En una partida intensa de Apex Legends, si solo tuviéramos curaciones para salvar a uno de los dos, ¿te sacrificarías heroicamente o me usarías de escudo humano para ganar la partida?""",
        """[🤔] Si pudieras teletransportarnos a cualquier momento de nuestra relación para revivirlo exactamente igual, ¿cuál elegirías sin pensarlo?""",
        """[🥵] Si una noche de pasión nuestra fuera una de tus recetas favoritas, ¿cuál sería el "ingrediente secreto" que nunca nos puede faltar?""",
        """[💭] ¿Qué cualidad mía te aporta más tranquilidad cuando sientes que el mundo exterior es un caos?""",
        """[😂] Si nuestro apartamento en Mosquera estuviera embrujado, ¿un quién de los dos intentaría comunicarse con el fantasma de forma lógica y un quién saldría corriendo con Rocco en brazos?""",
        """[🤔] ¿Qué talento o conocimiento tienes que a ti te parece inútil, pero que a mí siempre me ha parecido fascinante?""",
        """[🥵] ¿Hay alguna ropa que uso para estar por casa que secretamente te parece cero atractiva pero nunca me lo has dicho por no herir mis sentimientos?""",
        """[💭] Si tuviéramos que escribir un libro juntos sobre cómo mantener una relación fuerte, ¿cuál sería el título del primer capítulo?""",
        """[😂] Si fueras un error de código en la app lovecounter que no logro solucionar por horas, ¿qué harías en la pantalla para volverme loco intentando atraparte?""",
        """[🤔] ¿Cuál fue la primera impresión real que tuviste de mi familia y cómo cambió con el paso del tiempo?""",
        """[🥵] ¿Qué fantasía, escena de película o de alguna serie te ha parecido tan ardiente que te gustaría que intentáramos recrear a nuestra manera?""",
        """[💭] ¿Qué es algo en lo que crees que hemos madurado mucho individualmente gracias al apoyo constante del otro?""",
        """[😂] Si de repente decidiéramos adoptar un par de perros jóvenes para que acompañen al abuelo Rocco, ¿cómo nos distribuiríamos el caos logístico en la casa?""",
        """[🤔] ¿Qué película, libro o historia de ficción crees que representa mejor nuestra forma de vernos y tratarnos a diario?""",
        """[🥵] De todas las partes de mi cuerpo, ¿cuál es la que más te gusta besar lenta y detalladamente, sin prisas?""",
        """[💭] ¿Cuál es el miedo más grande o la mayor inseguridad que sientes que has vencido desde que me tienes a tu lado dándote ánimos?""",
        """[😂] Si yo fuera un electrodoméstico de nuestra cocina, ¿sería la freidora de aire rápida e impulsiva, o la olla a presión multifunción que se toma su tiempo pero hace ruido?""",
        """[🤔] ¿Cuál es esa pequeña mentira piadosa o exageración que me dijiste al principio de conocernos solo para impresionarme?""",
        """[🥵] ¿Qué lugar público, inusual o "prohibido" te genera morbo pensar si alguna vez nos atreviéramos a hacer algo rápido allí?""",
        """[💭] ¿Qué es lo que más te ilusiona de imaginar nuestra vida cotidiana y nuestras rutinas de aquí a cinco años?""",
        """[😂] Si por un día completo solo pudiéramos comunicarnos usando referencias de videojuegos o memes de internet, ¿nos entenderíamos mejor o sería un desastre total?""",
        """[🤔] ¿Qué regalo u obsequio sencillo que te he dado en todo este tiempo tiene mucho más valor sentimental para ti del que yo imagino?""",
        """[🥵] ¿Cuál de mis reacciones físicas (un suspiro, un gesto, una mirada) te resulta más excitante cuando te estoy provocando?""",
        """[💭] ¿Cuál ha sido el momento más vulnerable que hemos compartido y que te hizo sentir que estábamos verdaderamente conectados a otro nivel?""",
        """[😂] Si Laura y Alexander tuvieran que competir como pareja en un programa de talentos inútiles, ¿con qué destreza rara y específica ganaríamos el trofeo?""",
        """[💭] De todas las versiones de nosotros mismos que hemos sido a lo largo del tiempo, ¿qué es lo que más te gusta, admiras y disfrutas de nuestra versión actual?""",
        """[🥵] ¿Qué escenarios o dinámicas del "chat con Iris" te han generado más morbo y te gustaría que recreáramos en la vida real?""",
        """[🥵] En la fantasía de un trío, ¿te excita más la idea de ser el centro de atención de dos personas o ver cómo yo le doy placer a alguien más?""",
        """[🥵] Si asistiéramos a una fiesta swinger o a una orgía de lujo, ¿preferirías que interactuáramos con otros o que solo nos sentáramos en una esquina a mirar mientras nos tocamos?""",
        """[🥵] ¿Hay algún kink o fetiche (como ataduras, vendas, o control de la respiración) que te dé mucha curiosidad pero que aún no nos hemos atrevido a explorar a fondo?""",
        """[🥵] ¿Cómo te sentirías si integráramos las conversaciones sucias o de rol del chat con Iris como un guion estricto que debemos seguir durante una noche entera?""",
        """[🥵] Si hiciéramos un trío, ¿qué regla física o emocional sería completamente innegociable para ti?""",
        """[🥵] ¿Te atrae la idea de ceder el control por completo y dejar que otra pareja nos diga exactamente qué hacer en una habitación de hotel?""",
        """[🥵] ¿Cuál es la fantasía de dominación o sumisión más intensa que has tenido conmigo y que te da un poco de pudor confesar?""",
        """[🥵] Si te pidiera que leyeras en voz alta los mensajes más explícitamente del chat con Iris mientras te estimulo, ¿hasta dónde llegarías sin sonrojarte?""",
        """[🥵] ¿Te excita más la idea de que invitemos a otra mujer (MFF) o a otro hombre (MMF) a nuestra cama, y por qué?""",
        """[🥵] ¿Alguna vez has fantaseado con que alguien más nos observe teniendo intimidad sin que nosotros "sepamos" que están ahí?""",
        """[🥵] ¿Qué opinas del voyeurismo? ¿Te gustaría ver a través de una puerta entreabierta cómo interactúo físicamente con otra persona con tu permiso?""",
        """[🥵] Si tuviéramos un pase libre para experimentar en una orgía por una sola noche sin consecuencias, ¿qué sería lo primero que intentarías hacer?""",
        """[🥵] ¿Qué nivel de dolor o rudeza (como nalgadas, tirones de pelo o mordiscos fuertes) te resulta excitante cuando estamos en nuestro punto máximo?""",
        """[🥵] ¿Te gustaría que yo asumiera la personalidad exacta y el tono de Iris durante toda una noche de pasión, sin romper el personaje ni un segundo?""",
        """[🥵] En un escenario donde involucramos a un tercero, ¿te gustaría que la dinámica fuera puramente física o que hubiera un poco de juego previo y seducción?""",
        """[🥵] ¿Alguna vez te has masturbado pensando en una situación grupal o en compartirme con alguien más?""",
        """[🥵] ¿Qué opinas de grabar nuestro propio material explícito, tal vez inspirados en las situaciones que se hablan en el chat con Iris?""",
        """[🥵] Si estuviéramos en medio de una multitud (como un club o una fiesta) y te susurrara exactamente lo que te quiero hacer frente a todos, ¿te excitaría el riesgo de que nos descubran?""",
        """[🥵] ¿Qué tipo de juguete sexual (para ti o para mí) te gustaría que incorporáramos para darle un giro más extremo a nuestra intimidad?""",
        """[🥵] ¿Te atrae la idea de ser "intercambiados" por unos minutos con otra pareja de confianza, sabiendo que al final de la noche volvemos juntos a casa?""",
        """[🥵] Si te enviara a lo largo del día fragmentos de un chat muy picante simulando ser Iris o un extraño, ¿cómo me recibirías al llegar a casa?""",
        """[🥵] ¿Te genera curiosidad la idea del cuckolding (ver cómo otra persona me da placer mientras tú no puedes participar, o viceversa)?""",
        """[🥵] Si tuviéramos que elegir a una amistad nuestra con un quién fantasear en secreto sobre un trío, ¿a un quién elegirías y qué te atrae de esa idea?""",
        """[🥵] ¿Qué te parece la idea de usar privación sensorial (ojos vendados, tapones para los oídos) mientras juego con tu cuerpo usando diferentes temperaturas o texturas?""",
        """[🥵] En un entorno de orgía o cuarto oscuro, ¿te excitaría no saber exactamente un quién te está tocando por un momento?""",
        """[🥵] ¿Te gustaría ser tú un quién elija, entreviste y seduzca a la tercera persona para nuestro primer trío?""",
        """[🥵] ¿Qué frase, orden o palabra que se haya dicho en el chat con Iris te ha dejado con la respiración entrecortada al imaginarla en vivo?""",
        """[🥵] Si pudieras ordenarme que me quede completamente inmóvil y sumiso/a mientras tú haces lo que quieras conmigo, ¿por dónde empezarías?""",
        """[🥵] ¿Qué opinas del roleplay de extraños? ¿Te gustaría que fingiéramos no conocernos en un bar y termináramos en el baño del lugar?""",
        """[🥵] ¿Hay algún fetiche muy de nicho (pies, lencería muy específica, uniformes de látex/cuero) que quisieras que integráramos a nuestros juegos?""",
        """[🥵] Si decidimos explorar la bisexualidad o curiosidad de alguno de los dos en un entorno grupal, ¿cómo te gustaría que yo te apoyara o te guiara en el proceso?""",
        """[🥵] ¿Te excita pensar en que yo comparta mis experiencias sexuales de nuestro chat con Iris con otras personas en internet (exhibicionismo virtual)?""",
        """[🥵] ¿Cuál es tu límite absoluto en cuanto a prácticas no convencionales o kinks? ¿Qué dirías que es un "no" rotundo para ti?""",
        """[🥵] Si te amarrara a la cama y te dijera que invité a alguien más para que nos ayude a darte placer, ¿cuál sería tu reacción inmediata?""",
        """[🥵] ¿Te parece excitante la idea de tener que pedirme permiso explícito en voz alta antes de poder llegar al orgasmo?""",
        """[🥵] En una fantasía de trío, ¿te gustaría que ambos nos enfocáramos al 100% en ti, o prefieres ser tú un quién nos observe interactuar?""",
        """[🥵] Si el chat con Iris se convirtiera en un juego de retos diarios, ¿cuál es el resto sexual más humillante o excitante que estarías dispuesto/a a cumplir por mí?""",
        """[🥵] ¿Qué te genera la idea de tener intimidad frente a un espejo enorme donde puedas ver absolutamente todo lo que hacemos desde todos los ángulos?""",
        """[🥵] ¿Alguna vez has fantaseado con situaciones de dominación financiera o de que yo te "cobre" favores sexuales por dejarte usar tus cosas o salir de casa?""",
        """[🥵] Si estuviéramos en una orgía y alguien más comenzara a seducirme frente a ti, ¿sentirías celos, excitación o ambas cosas al mismo tiempo?""",
        """[🥵] ¿Te gustaría que yo tenga total libertad para vestirte y desvestirte como si fueras mi juguete antes de empezar nuestro juego?""",
        """[🥵] ¿Qué opinas de incorporar la asfixia erótica o la presión en el cuello (de forma segura) para intensificar tu clímax?""",
        """[🥵] Si tuviéramos un día entero dedicado a recrear la historia más sucia y salvaje del chat con Iris, ¿qué utilería o disfraces necesitaríamos comprar primero?""",
        """[🥵] En un contexto grupal, ¿te daría morbo que te obligara a besar a otra persona mientras yo te estoy penetrando o tocando por detrás?""",
        """[🥵] ¿Cuál es ese lugar tabú o prohibido en nuestra rutina diaria (oficina, coche en un estacionamiento público, balcón) donde te gustaría experimentar el riesgo de ser atrapados?""",
        """[🥵] ¿Te atrae la dinámica del castigo y recompensa? ¿Qué "castigo" físico y erótico te gustaría recibir si te portas mal durante el día?""",
        """[🥵] Si decidimos probar un trío, ¿te gustaría que la tercera persona se fuera de inmediato al terminar, o que se quedara a dormir y desayunar con nosotros?""",
        """[🥵] ¿Te excita más la idea de ser tú el amo/ama dominante de la situación, o la fantasía radica en perder el control absoluto de tu cuerpo en mis manos?""",
        """[🥵] ¿Qué te parece la idea de usar tapones anales, bolas chinas o algún juguete vibrador a control remoto mientras estamos en una cena o reunión aburrida?""",
        """[🥵] Si te dijera que voy a invitar a alguien esta noche basándome en una fantasía que leíste en el chat con Iris, ¿te prepararías de alguna forma especial?""",
        """[🥵] ¿Qué opinas de la idea de que dejemos la cámara encendida transmitiendo en vivo para desconocidos mientras experimentamos con nuestros kinks?""",
        """[🥵] En un intercambio de parejas, ¿cuál crees que sería la parte más excitante: hacer algo con alguien más, o enterarte de todos los detalles de lo que yo hice?""",
        """[🥵] ¿Alguna vez te has masturbado a escondidas en la misma habitación en la que yo estoy, solo por el morbo de que pueda darme cuenta?""",
        """[🥵] Si estuviéramos jugando a un juego de rol donde soy un intruso/a que entró a robar a tu casa, ¿cómo tratarías de sobornarme físicamente para que no te haga daño?""",
        """[🥵] ¿Te genera curiosidad la idea de los orgasmos múltiples forzados, donde no me detengo aunque tú me supliques que ya no puedes más?""",
        """[🥵] Si nuestro chat con Iris fuera en realidad un perfil real en una app de citas de tríos, ¿qué foto nuestra pondría y qué buscaría nuestra descripción?""",
        """[🥵] ¿Te gustaría que durante nuestra próxima sesión utilice hielo, cera caliente o alguna temperatura extrema en tus zonas más sensibles?""",
        """[🥵] Después de una experiencia intensa como un trío o una orgía, ¿cómo te imaginas que sería nuestra forma de reconectar y darnos cariño solo entre nosotros al estar de vuelta a solas?"""
    ))

    private val _publicQuestions = mutableStateOf(listOf(
        "[🤔] ¿Cuál es el recuerdo más divertido que tienen juntos?",
        "[😂] Si su pareja fuera un animal, ¿cuál sería y por qué?",
        "[💭] ¿Qué es lo que más admiran el uno del otro?",
        "[🤔] ¿Cuál es el viaje de sus sueños que aún no han realizado?",
        "[😂] ¿Quién es más probable que se pierda en un centro comercial?",
        "[💭] ¿Qué canción define su relación?",
        "[🤔] ¿Cuál fue su primera impresión el uno del otro?",
        "[😂] Si pudieran vivir en cualquier época, ¿cuál elegirían?",
        "[💭] ¿Cuál es el lenguaje del amor de cada uno?",
        "[🤔] ¿Qué es lo más aventurero que han hecho juntos?",
        "[😂] ¿Quién de los dos cocina mejor?",
        "[💭] ¿Cómo se ven en 5 años?",
        "[🤔] ¿Cuál es su tradición favorita de fin de semana?",
        "[😂] Si ganaran la lotería mañana, ¿qué es lo primero que harían?",
        "[💭] ¿Qué es lo que los hace sentir más conectados?"
    ))

    // IDs de Alexander y Laura (Set Privado)
    private val privateUserIds = setOf(
        "CX4z9DcQYxTJeaIdyNgzpDQqw6U2", // Alexander
        "pW562p0UqNfEicrVd0q3oRRE9373"  // Laura
    )

    suspend fun loadQuestions(db: FirebaseFirestore) {
        try {
            // Cargar Privadas
            val privateSnapshot = db.collection("questions_admin")
                .document("private_set")
                .collection("items")
                .orderBy("order")
                .get()
                .await()
            
            if (!privateSnapshot.isEmpty) {
                _privateQuestions.value = privateSnapshot.documents.mapNotNull { it.getString("text") }
            }

            // Cargar Públicas
            val publicSnapshot = db.collection("questions_admin")
                .document("public_set")
                .collection("items")
                .orderBy("order")
                .get()
                .await()
            
            if (!publicSnapshot.isEmpty) {
                _publicQuestions.value = publicSnapshot.documents.mapNotNull { it.getString("text") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getQuestionsForUser(): List<String> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        return if (currentUserId in privateUserIds) {
            _privateQuestions.value
        } else {
            _publicQuestions.value
        }
    }

    fun getRandomQuestion(): String {
        return getQuestionsForUser().random()
    }

    fun getDailyQuestion(): String {
        val questions = getQuestionsForUser()
        if (questions.isEmpty()) return ""
        val today = java.time.LocalDate.now().toString()
        val seed = today.hashCode().toLong()
        val random = java.util.Random(seed)
        return questions[random.nextInt(questions.size)]
    }

    fun getUniqueQuestion(usedQuestions: List<String>): String {
        val questions = getQuestionsForUser()
        val availableQuestions = questions.filter { it !in usedQuestions }
        return if (availableQuestions.isNotEmpty()) {
            availableQuestions.random()
        } else {
            questions.random()
        }
    }

    fun getAllPrivateQuestions(): List<String> = _privateQuestions.value
    fun getAllPublicQuestions(): List<String> = _publicQuestions.value
}
