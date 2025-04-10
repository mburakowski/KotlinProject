import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.time.LocalDate

interface Loginable {
    fun loginUser(password: String): String
}

interface Registerable {
    fun registerUser(): String
}

interface Reviews {
    fun addRev(review: String, star: Int)
    fun viewReviews(): List<String>
}

abstract class User(
    val id: Int,
    internal val login: String,
    protected val email: String,
    protected val registerDate: String,
    protected val hashedPassword: String
) {
    fun getUserInfo(): String {
        return "ID: $id, Login: $login, Email: $email, Rejestracja: $registerDate"
    }

    fun checkPassword(inputPassword: String): Boolean {
        return hashedPassword == hashPassword(inputPassword)
    }

    abstract fun userType(): String
    abstract fun getRawData(): String
}

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

    override fun userType(): String = "Kupujacy"

    override fun loginUser(password: String): String {
        return if (checkPassword(password)) {
            "Kupujacy $login zalogowal sie!"
        } else {
            "Błąd logowania – niepoprawne hasło!"
        }
    }

    override fun registerUser(): String = "Kupujacy $login zostal zarejestrowany!"

    override fun getRawData(): String {
        return "$id,$login,$email,$registerDate,$hashedPassword"
    }
}

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

    override fun userType(): String = "Sprzedawca"

    override fun loginUser(password: String): String {
        return if (checkPassword(password)) {
            "Sprzedawca $login zalogowal sie!"
        } else {
            "Niepoprawne haslo!"
        }
    }

    override fun registerUser(): String = "Sprzedawca $login zostal zarejestrowany!"

    override fun getRawData(): String {
        return "$id,$login,$email,$registerDate,$hashedPassword"
    }
}

val USERS_FILE = "users.txt"
val PRODUCTS_FILE = "products.txt"

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

fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(password.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

////////////


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

fun processPayment(seller: Seller, amount: Double) {
    println("Sprzedawca ${seller.login} zarobił $amount PLN")
    seller.balance += amount
}

class BuyerReview : Reviews {
    private val reviews = mutableListOf<String>()

    override fun addRev(review: String, star: Int) {
        val rating = "★".repeat(star) + "☆".repeat(5 - star)
        val fullReview = "Opinia: $review, Ocena: $rating"
        reviews.add(fullReview)
        println(fullReview)
    }

    override fun viewReviews(): List<String> {
        return reviews
    }
}

fun viewReviews(buyerReview: BuyerReview) {
    println("Opinie użytkownika:")
    for (review in buyerReview.viewReviews()) {
        println(review)
    }
}

class Marketplace {
    private val users = mutableListOf<User>()
    val products = mutableListOf<Product>()

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

    fun addProduct(product: Product) {
        products.add(product)
    }

    fun makePurchase(buyer: Buyer, product: Product) {
        if (buyer.balance >= product.cena) {
            buyer.balance -= product.cena
            product.seller.balance += product.cena
            println("Zakupiono '${product.nazwa}' za ${product.cena} PLN.")
            println("Nowe saldo kupującego: ${buyer.balance} PLN")
            println("Nowe saldo sprzedawcy: ${product.seller.balance} PLN")
        } else {
            println("Kupujący nie ma wystarczających środków na koncie.")
        }
    }
}

fun saveProductsToFile(products: List<Product>) {
    File(PRODUCTS_FILE).printWriter().use { out ->
        for (product in products) {
            // Format: nazwa,cena,opis,sellerLogin
            out.println("${product.nazwa},${product.cena},${product.opis},${product.seller.login}")
        }
    }
}

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
        println("0. Wyjście")
        print("Wybierz opcję: ")

        when (scanner.nextLine()) {
            "1" -> {
                println("\n--- Rejestracja ---")
                print("Login: ")
                val login = scanner.nextLine()
                print("Email: ")
                val email = scanner.nextLine()
                print("Hasło: ")
                val password = scanner.nextLine()
                print("Typ użytkownika (kupujący/sprzedawca): ")
                val type = scanner.nextLine()
                val registerDate = LocalDate.now().toString()

                val user = when (type.lowercase()) {
                    "kupujący" -> Buyer(userId++, login, email, registerDate, password)
                    "sprzedawca" -> {
                        print("Podaj kod autoryzacyjny sprzedawcy: ")
                        val code = scanner.nextLine()
                        if (code != sellerCode) {
                            println("Niepoprawny kod. Rejestracja sprzedawcy przerwana.")
                            continue
                        }
                        Seller(userId++, login, email, registerDate, password)
                    }
                    else -> {
                        println("Nieznany typ użytkownika.")
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
                print("Hasło: ")
                val password = scanner.nextLine()
                val foundUser = users.find { it.login == login }

                if (foundUser != null && foundUser is Loginable) {
                    val result = foundUser.loginUser(password)
                    println(result)
                    println("Informacje: ${foundUser.getUserInfo()}")

                    // Jeśli sprzedawca, pokaż menu dodawania produktów
                    if (foundUser is Seller) {
                        while (true) {
                            println("\n--- MENU SPRZEDAWCY ---")
                            println("1. Dodaj produkt")
                            println("2. Pokaż produkty")
                            println("3. Wypłać środki")
                            println("0. Wyloguj")
                            print("Wybierz opcję: ")
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
                                    print("Kwota do wypłaty: ")
                                    val kwota = scanner.nextLine().toDoubleOrNull()
                                    if (kwota != null && kwota <= foundUser.balance) {
                                        foundUser.balance -= kwota
                                        println("Wypłacono $kwota PLN. Pozostałe saldo: ${foundUser.balance} PLN")
                                        // Zapisz zaktualizowanych użytkowników (nowe salda)
                                        saveUsersToFile(users)
                                    } else println("Niepoprawna kwota lub brak środków.")
                                }
                                "0" -> break
                                else -> println("Nieznana opcja.")
                            }
                        }
                    } else if (foundUser is Buyer) {
                        while (true) {
                            println("\n--- MENU KUPUJĄCEGO ---")
                            println("1. Pokaż produkty")
                            println("2. Kup produkt")
                            println("3. Pokaż saldo")
                            println("4. Doładuj saldo")
                            println("5. Wystaw opinię")
                            println("6. Pokaż opinie")
                            println("0. Wyloguj")
                            print("Wybierz opcję: ")
                            when (scanner.nextLine()) {
                                "1" -> marketplace.showProducts()
                                "2" -> {
                                    if (marketplace.products.isEmpty()) {
                                        println("Brak dostępnych produktów.")
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
                                    } else println("Nieprawidłowy wybór.")
                                }
                                "3" -> println("Saldo: ${foundUser.balance} PLN")
                                "4" -> {
                                    print("Kwota do doładowania: ")
                                    val kwota = scanner.nextLine().toDoubleOrNull()
                                    if (kwota != null && kwota > 0) {
                                        foundUser.balance += kwota
                                        println("Doładowano $kwota PLN. Nowe saldo: ${foundUser.balance} PLN")
                                        saveUsersToFile(users)
                                    } else println("Niepoprawna kwota.")
                                }
                                "5" -> {
                                    print("Opinia: ")
                                    val text = scanner.nextLine()
                                    print("Ocena (1-5): ")
                                    val rating = scanner.nextLine().toIntOrNull()
                                    if (rating in 1..5) foundUser.buyerReview.addRev(text, rating!!)
                                    else println("Nieprawidłowa ocena.")
                                }
                                "6" -> viewReviews(foundUser.buyerReview)
                                "0" -> break
                                else -> println("Nieznana opcja.")
                            }
                        }
                    }
                } else {
                    println("Nie znaleziono użytkownika.")
                }
            }
            "0" -> {
                println("Zakończono program.")
                break
            }
            else -> println("Nieprawidłowy wybór.")
        }
    }
}
