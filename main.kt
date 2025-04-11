import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.time.LocalDate


/**
 * Interfejs definiujący możliwość logowania użytkownika.
 */
interface Loginable {
    /**
     * Loguje użytkownika na podstawie podanego hasła.
     *
     * @param password Hasło użytkownika.
     * @return Komunikat o statusie logowania.
     */
    fun loginUser(password: String): String
}

/**
 * Interfejs definiujący możliwość rejestracji użytkownika.
 */
interface Registerable {
    /**
     * Rejestruje nowego użytkownika.
     *
     * @return Komunikat o zakończeniu procesu rejestracji.
     */
    fun registerUser(): String
}

/**
 * Interfejs opinii – pozwala użytkownikowi dodawać i przeglądać recenzje.
 */
interface Reviews {
    /**
     * Zwraca listę wszystkich opinii użytkownika.
     *
     * @return Lista opinii.
     */
    fun viewReviews(): List<String>
}

/**
 * Klasa bazowa reprezentująca użytkownika. Zawiera dane logowania, hasło oraz datę rejestracji.
 *
 * @param id Identyfikator użytkownika.
 * @param login Nazwa użytkownika (login).
 * @param email Adres e-mail użytkownika.
 * @param registerDate Data rejestracji użytkownika.
 * @param hashedPassword Zaszyfrowane hasło użytkownika.
 */
abstract class User(
    val id: Int,
    internal val login: String,
    protected val email: String,
    protected val registerDate: String,
    protected val hashedPassword: String
) {
    /**
     * Zwraca informacje o użytkowniku w postaci tekstowej.
     *
     * @return Informacje o użytkowniku.
     */
    fun getUserInfo(): String {
        return "ID: $id, Login: $login, Email: $email, Rejestracja: $registerDate"
    }

    /**
     * Sprawdza, czy podane hasło jest zgodne z zapisanym w systemie hasłem.
     *
     * @param inputPassword Hasło podane przez użytkownika.
     * @return `true` jeśli hasło jest zgodne, `false` w przeciwnym razie.
     */
    fun checkPassword(inputPassword: String): Boolean {
        return hashedPassword == hashPassword(inputPassword)
    }

    abstract fun userType(): String
    abstract fun getRawData(): String
}

/**
 * Klasa reprezentująca kupującego.
 */
class Buyer(
    id: Int,
    login: String,
    email: String,
    registerDate: String,
    password: String,
    private val purchaseCount: Int = 0,
    hashed: Boolean = false,
    var balance: Double = 0.0,
) : User(id, login, email, registerDate, if (hashed) password else hashPassword(password)), Loginable, Registerable {

    val buyerReview = BuyerReview()

    /**
     * Zwraca typ użytkownika, np. "Kupujacy" lub "Sprzedawca".
     *
     * @return Typ użytkownika.
     */
    override fun userType(): String = "Kupujacy"

    override fun loginUser(password: String): String {
        return if (checkPassword(password)) {
            "Kupujacy $login zalogowal sie!"
        } else {
            "Błąd logowania – niepoprawne hasło!"
        }
    }

    override fun registerUser(): String = "Kupujacy $login zostal zarejestrowany!"

    /**
     * Zwraca dane użytkownika w surowym formacie (do zapisu do pliku).
     *
     * @return Surowe dane użytkownika.
     */
    override fun getRawData(): String {
        return "$id,$login,$email,$registerDate,$hashedPassword"
    }
}

/**
 * Klasa reprezentująca kupującego w systemie.
 *
 * @param id Identyfikator użytkownika.
 * @param login Nazwa użytkownika.
 * @param email Adres e-mail użytkownika.
 * @param registerDate Data rejestracji użytkownika.
 * @param password Hasło użytkownika.
 * @param soldItems Liczba dokonanych zakupów (domyślnie 0).
 * @param hashed Flaga określająca, czy hasło jest zaszyfrowane.
 * @param balance Saldo konta kupującego (domyślnie 0.0).
 */
class Seller(
    id: Int,
    login: String,
    email: String,
    registerDate: String,
    password: String,
    private val soldItems: Int = 0,
    hashed: Boolean = false,
    var balance: Double = 0.0
) : User(id, login, email, registerDate, if (hashed) password else hashPassword(password)), Loginable, Registerable {

    /**
     * Zwraca typ użytkownika ("Kupujacy").
     *
     * @return Typ użytkownika.
     */
    override fun userType(): String = "Sprzedawca"

    /**
     * Loguje użytkownika na podstawie podanego hasła.
     *
     * @param password Hasło użytkownika.
     * @return Komunikat o statusie logowania.
     */
    override fun loginUser(password: String): String {
        return if (checkPassword(password)) {
            "Sprzedawca $login zalogowal sie!"
        } else {
            "Niepoprawne haslo!"
        }
    }

    /**
     * Rejestruje użytkownika jako kupującego.
     *
     * @return Komunikat o zakończeniu rejestracji.
     */
    override fun registerUser(): String = "Sprzedawca $login zostal zarejestrowany!"

    /**
     * Zwraca dane użytkownika w surowym formacie (do zapisu do pliku).
     *
     * @return Surowe dane użytkownika.
     */
    override fun getRawData(): String {
        return "$id,$login,$email,$registerDate,$hashedPassword"
    }
}

val USERS_FILE = "users.txt"
val PRODUCTS_FILE = "products.txt"

/**
 * Zapisuje użytkowników do pliku tekstowego.
 * @param users Lista użytkowników do zapisania.
 */
fun saveUsersToFile(users: List<User>) {
    File(USERS_FILE).printWriter().use { out ->
        for (user in users) {
            val line = when (user) {
                is Buyer -> "Buyer,${user.getRawData()}"
                is Seller -> "Seller,${user.getRawData()}"
                else -> continue
            }
            out.println(line)
        }
    }
}

/**
 * Wczytuje użytkowników z pliku tekstowego.
 * @return Lista wczytanych użytkowników.
 */
fun loadUsersFromFile(): MutableList<User> {
    val users = mutableListOf<User>()
    if (!File(USERS_FILE).exists()) return users

    File(USERS_FILE).readLines().forEach { line ->
        val parts = line.split(",")
        if (parts.size >= 6) {
            val type = parts[0]
            val id = parts[1].toInt()
            val login = parts[2]
            val email = parts[3]
            val date = parts[4]
            val hashedPassword = parts[5]

            when (type) {
                "Buyer" -> users.add(Buyer(id, login, email, date, hashedPassword, 0, true))
                "Seller" -> users.add(Seller(id, login, email, date, hashedPassword, 0, true))
            }
        }
    }

    return users
}

/**
 * Funkcja do haszowania hasła użytkownika.
 *
 * @param password Hasło do zaszyfrowania.
 * @return Zaszyfrowane hasło.
 */
fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(password.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

////////////

/**
 * Klasa reprezentująca produkt w sklepie.
 * @property nazwa Nazwa produktu.
 * @property cena Cena produktu.
 * @property opis Opis produktu.
 * @property seller Sprzedawca oferujący ten produkt.
 */
class Product(
    var nazwa: String,
    var cena: Double,
    var opis: String,
    val seller: Seller
) {
    override fun toString(): String {
        return "$nazwa - $opis ($cena PLN)"
    }
}

/**
 * Klasa do wystawiania i przeglądania opinii użytkownika.
 */
class BuyerReview : Reviews {
    private val sellerReviews = mutableMapOf<String, MutableList<String>>() // login sprzedawcy -> opinie

    fun addRevForSeller(sellerLogin: String, review: String, star: Int) {
        val rating = "★".repeat(star) + "☆".repeat(5 - star)
        val fullReview = "Opinia: $review, Ocena: $rating"

        val reviews = sellerReviews.getOrPut(sellerLogin) { mutableListOf() }
        reviews.add(fullReview)

        println("Dodano opinię dla sprzedawcy '$sellerLogin': $fullReview")
    }

    fun viewReviewsForSeller(sellerLogin: String): List<String> {
        return sellerReviews[sellerLogin] ?: listOf("Brak opinii dla tego sprzedawcy.")
    }

    override fun viewReviews(): List<String> {
        // Można zwrócić wszystkie opinie dla wszystkich sprzedawców (opcjonalnie)
        return sellerReviews.flatMap { (seller, reviews) ->
            reviews.map { "Sprzedawca: $seller, $it" }
        }
    }
}

/**
 * Główna klasa systemu sklepu – zarządza użytkownikami i produktami.
 */
class Marketplace {
    private val users = mutableListOf<User>()
    val products = mutableListOf<Product>()

    /**
     * Rejestruje nowego użytkownika.
     */
    fun registerUser(user: User) {
        users.add(user)
        println("${user.login} został zarejestrowany.")
    }

    fun showProducts() {
        println("Dostępne produkty:")
        for (product in products) {
            println(product)
        }
    }

    /**
     * Dodaje produkt do listy dostępnych.
     */
    fun addProduct(product: Product) {
        products.add(product)
    }

    /**
     * Realizuje zakup produktu przez kupującego.
     */
    fun makePurchase(buyer: Buyer, product: Product) {
        if (buyer.balance >= product.cena) {
            buyer.balance -= product.cena
            product.seller.balance += product.cena
            println("Zakupiono '${product.nazwa}' za ${product.cena} PLN.")
            println("Nowe saldo kupującego: ${buyer.balance} PLN")
        } else {
            println("Brak wystarczających środków na koncie.")
        }
    }
}

/**
 * Zapisuje produkty do pliku tekstowego.
 */
fun saveProductsToFile(products: List<Product>) {
    File(PRODUCTS_FILE).printWriter().use { out ->1
        for (product in products) {
            // Format: nazwa,cena,opis,sellerLogin
            out.println("${product.nazwa},${product.cena},${product.opis},${product.seller.login}")
        }
    }
}

/**
 * Wczytuje produkty z pliku tekstowego i przypisuje je sprzedawcom.
 */
fun loadProductsFromFile(users: List<User>): MutableList<Product> {
    val products = mutableListOf<Product>()
    if (!File(PRODUCTS_FILE).exists()) return products

    File(PRODUCTS_FILE).readLines().forEach { line ->
        val parts = line.split(",")
        if (parts.size >= 4) {
            val nazwa = parts[0]
            val cena = parts[1].toDoubleOrNull() ?: return@forEach
            val opis = parts[2]
            val sellerLogin = parts[3]
            val seller = users.find { it.login == sellerLogin && it is Seller } as? Seller
            if (seller != null) {
                products.add(Product(nazwa, cena, opis, seller))
            }
        }
    }

    return products
}

/**
 * Funkcja główna – uruchamia menu programu.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun main() {
    val scanner = Scanner(System.`in`)
    val users = loadUsersFromFile()
    var userId = (users.maxOfOrNull { it.id } ?: 0) + 1
    val sellerCode = "admin"
    val products = loadProductsFromFile(users)
    val marketplace = Marketplace()

    // Załaduj użytkowników i produkty do marketplace
    users.forEach { marketplace.registerUser(it) }
    products.forEach { marketplace.addProduct(it) }

    while (true) {
        println("\n==== MENU ====")
        println("1. Rejestracja")
        println("2. Logowanie")
        println("0. Wyjscie")
        print("Wybierz opcje: ")

        when (scanner.nextLine()) {
            "1" -> {
                println("\n--- Rejestracja ---")
                print("Login: ")
                val login = scanner.nextLine()
                print("Email: ")
                val email = scanner.nextLine()
                print("Hasło: ")
                val password = scanner.nextLine()

                println("Typ uzytkownika:")
                println("1. Kupujacy")
                println("2. Sprzedawca")
                print("Wybierz 1 lub 2: ")
                val typeOption = scanner.nextLine()
                val registerDate = LocalDate.now().toString()

                val user = when (typeOption) {
                    "1" -> Buyer(userId++, login, email, registerDate, password)
                    "2" -> {
                        print("Podaj kod autoryzacyjny sprzedawcy: ")
                        val code = scanner.nextLine()
                        if (code != sellerCode) {
                            println("Niepoprawny kod. Rejestracja sprzedawcy przerwana.")
                            continue
                        }
                        Seller(userId++, login, email, registerDate, password)
                    }
                    else -> {
                        println("Nieznana opcja.")
                        continue
                    }
                }

                users.add(user)
                marketplace.registerUser(user)
                saveUsersToFile(users)
                println(user.registerUser())
            }

            "2" -> {
                println("\n--- Logowanie ---")
                print("Login: ")
                val login = scanner.nextLine()
                print("Haslo: ")
                val password = scanner.nextLine()
                val foundUser = users.find { it.login == login }

                if (foundUser != null && foundUser is Loginable) {
                    val result = foundUser.loginUser(password)
                    println(result)

                    if (!foundUser.checkPassword(password)) {
                        println("Logowanie nie powiodło sie – nieprawidlowe haslo.")
                        continue
                    }

                    println("Informacje: ${foundUser.getUserInfo()}")

                    // Jeśli sprzedawca, pokaż menu dodawania produktów
                    if (foundUser is Seller) {
                        while (true) {
                            println("\n--- MENU SPRZEDAWCY ---")
                            println("1. Dodaj produkt")
                            println("2. Pokaz produkty")
                            println("3. Wyplac srodki")
                            println("0. Wyloguj")
                            print("Wybierz opcje: ")
                            when (scanner.nextLine()) {
                                "1" -> {
                                    print("Nazwa produktu: ")
                                    val nazwa = scanner.nextLine()
                                    print("Opis: ")
                                    val opis = scanner.nextLine()
                                    print("Cena: ")
                                    val cena = scanner.nextLine().toDoubleOrNull()
                                    if (cena == null || cena < 0) {
                                        println("Niepoprawna cena!")
                                        continue
                                    }
                                    val produkt = Product(nazwa, cena, opis, foundUser)
                                    marketplace.addProduct(produkt)
                                    saveProductsToFile(marketplace.products)
                                    println("Produkt dodany!")
                                }
                                "2" -> marketplace.showProducts()
                                "3" -> {
                                    print("Kwota do wyplaty: ")
                                    val kwota = scanner.nextLine().toDoubleOrNull()
                                    if (kwota != null && kwota <= foundUser.balance) {
                                        foundUser.balance -= kwota
                                        println("Wyplacono $kwota PLN. Pozostale saldo: ${foundUser.balance} PLN")
                                        // Zapisz zaktualizowanych użytkowników (nowe salda)
                                        saveUsersToFile(users)
                                    } else println("Niepoprawna kwota lub brak srodkow.")
                                }
                                "0" -> break
                                else -> println("Nieznana opcja.")
                            }
                        }
                    } else if (foundUser is Buyer) {
                        while (true) {
                            println("\n--- MENU KUPUJACEGO ---")
                            println("1. Pokaz produkty")
                            println("2. Kup produkt")
                            println("3. Pokaz saldo")
                            println("4. Doladuj saldo")
                            println("5. Wystaw opinie")
                            println("6. Pokaz opinie")
                            println("0. Wyloguj")
                            print("Wybierz opcję: ")
                            when (scanner.nextLine()) {
                                "1" -> marketplace.showProducts()
                                "2" -> {
                                    if (marketplace.products.isEmpty()) {
                                        println("Brak dostepnych produktow.")
                                        continue
                                    }
                                    for ((i, p) in marketplace.products.withIndex()) {
                                        println("${i + 1}. ${p.nazwa} - ${p.opis} (${p.cena} PLN) - ${p.seller.login}")
                                    }
                                    print("Wybierz numer produktu: ")
                                    val idx = scanner.nextLine().toIntOrNull()
                                    if (idx in 1..marketplace.products.size) {
                                        val prod = marketplace.products[idx!! - 1]
                                        marketplace.makePurchase(foundUser, prod)
                                        saveUsersToFile(users)
                                    } else println("Nieprawidlowy wybor.")
                                }
                                "3" -> println("Saldo: ${foundUser.balance} PLN")
                                "4" -> {
                                    print("Kwota do doladowania: ")
                                    val kwota = scanner.nextLine().toDoubleOrNull()
                                    if (kwota != null && kwota > 0) {
                                        foundUser.balance += kwota
                                        println("Doladowano $kwota PLN. Nowe saldo: ${foundUser.balance} PLN")
                                        saveUsersToFile(users)
                                    } else println("Niepoprawna kwota.")
                                }
                                "5" -> {
                                    if (marketplace.products.isEmpty()) {
                                        println("Brak produktow do oceny.")
                                        continue
                                    }

                                    println("Dostepni sprzedawcy produktow:")
                                    val sellers = marketplace.products.map { it.seller }.distinctBy { it.login }
                                    for ((i, s) in sellers.withIndex()) {
                                        println("${i + 1}. ${s.login}")
                                    }

                                    print("Wybierz numer sprzedawcy, którego chcesz ocenic: ")
                                    val sellerIdx = scanner.nextLine().toIntOrNull()
                                    if (sellerIdx !in 1..sellers.size) {
                                        println("Nieprawidlowy wybor sprzedawcy.")
                                        continue
                                    }
                                    val selectedSeller = sellers[sellerIdx!! - 1]

                                    print("Opinia: ")
                                    val text = scanner.nextLine()
                                    print("Ocena (1-5): ")
                                    val rating = scanner.nextLine().toIntOrNull()
                                    if (rating in 1..5) {
                                        foundUser.buyerReview.addRevForSeller(selectedSeller.login, text, rating!!)
                                    } else {
                                        println("Nieprawidlowa ocena.")
                                    }
                                }
                                "6" -> {
                                    val sellers = marketplace.products.map { it.seller }.distinctBy { it.login }
                                    if (sellers.isEmpty()) {
                                        println("Brak sprzedawcow do wyswietlenia opinii.")
                                        continue
                                    }

                                    println("Sprzedawcy z listy produktow:")
                                    for ((i, s) in sellers.withIndex()) {
                                        println("${i + 1}. ${s.login}")
                                    }

                                    print("Wybierz numer sprzedawcy, by zobaczyc opinie: ")
                                    val sellerIdx = scanner.nextLine().toIntOrNull()
                                    if (sellerIdx in 1..sellers.size) {
                                        val sellerLogin = sellers[sellerIdx!! - 1].login
                                        val reviews = foundUser.buyerReview.viewReviewsForSeller(sellerLogin)
                                        println("Opinie dla sprzedawcy '$sellerLogin':")
                                        reviews.forEach { println(it) }
                                    } else println("Nieprawidlowy wybór.")
                                }
                                "0" -> break
                                else -> println("Nieznana opcja.")
                            }
                        }
                    }
                } else {
                    println("Nie znaleziono uzytkownika.")
                }
            }
            "0" -> {
                println("Zakonczono program.")
                break
            }
            else -> println("Nieprawidlowy wybor.")
        }
    }
}
