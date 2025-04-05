import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.util.*

interface Loginable {
    fun loginUser(password: String): String
}

interface Registerable {
    fun registerUser(): String
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
    hashed: Boolean = false
) : User(id, login, email, registerDate, if (hashed) password else hashPassword(password)), Loginable, Registerable {

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
    hashed: Boolean = false
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

// w tym pliku zapisuja sie konta
val USERS_FILE = "users.txt"

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


fun main() {
    val scanner = Scanner(System.`in`)
    val users = loadUsersFromFile()
    var userId = (users.maxOfOrNull { it.id } ?: 0) + 1
    val sellerCode = "admin"

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

                print("Haslo: ")
                val password = scanner.nextLine()

                print("Typ uzytkownika (kupujacy/sprzedawca): ")
                val type = scanner.nextLine()

                val registerDate = LocalDate.now().toString()

                val user = when (type.lowercase()) {
                    "kupujacy" -> Buyer(userId++, login, email, registerDate, password)
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
                    println(foundUser.loginUser(password))
                    println("Informacje: ${foundUser.getUserInfo()}") //debug, mozna zakomentowac
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
