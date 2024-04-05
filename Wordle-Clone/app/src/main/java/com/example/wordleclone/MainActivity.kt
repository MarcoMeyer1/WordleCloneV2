package com.example.wordleclone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import android.widget.Button
import android.widget.Toast
import android.graphics.Color
import android.widget.TextView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class MainActivity : AppCompatActivity() {
    private lateinit var wordleApiService: WordleApiService
    private var currentWord: String = ""
    private var currentAttempt: Int = 1
    private lateinit var editTexts: Array<Array<EditText>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Retrofit to make API calls
        val retrofit = Retrofit.Builder()
            .baseUrl("https://wordlecloneapi.azurewebsites.net/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        wordleApiService = retrofit.create(WordleApiService::class.java)

        // Load the correct word from the API and populate it into the TextView
        loadCorrectWord()

        // Initialize the EditTexts for the guesses
        initializeEditTexts()

        // Set up the Enter button to submit a guess
        val btnEnter: Button = findViewById(R.id.btnEnter)
        btnEnter.setOnClickListener {
            val enteredWord = getCurrentGuess()
            if (enteredWord.length == 5) {
                checkWord(enteredWord)
            } else {
                Toast.makeText(this, "Enter a 5-letter word", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeEditTexts() {
        // Initialize EditTexts for user guesses
        editTexts = Array(6) { row ->
            when (row) {
                0 -> arrayOf(
                    findViewById(R.id.txtR1Letter1),
                    findViewById(R.id.txtR1Letter2),
                    findViewById(R.id.txtR1Letter3),
                    findViewById(R.id.txtR1Letter4),
                    findViewById(R.id.txtR1Letter5)
                )
                1 -> arrayOf(
                    findViewById(R.id.txtR2Letter1),
                    findViewById(R.id.txtR2Letter2),
                    findViewById(R.id.txtR2Letter3),
                    findViewById(R.id.txtR2Letter4),
                    findViewById(R.id.txtR2Letter5)
                )
                2 -> arrayOf(
                    findViewById(R.id.txtR3Letter1),
                    findViewById(R.id.txtR3Letter2),
                    findViewById(R.id.txtR3Letter3),
                    findViewById(R.id.txtR3Letter4),
                    findViewById(R.id.txtR3Letter5)
                )
                3 -> arrayOf(
                    findViewById(R.id.txtR4Letter1),
                    findViewById(R.id.txtR4Letter2),
                    findViewById(R.id.txtR4Letter3),
                    findViewById(R.id.txtR4Letter4),
                    findViewById(R.id.txtR4Letter5)
                )
                4 -> arrayOf(
                    findViewById(R.id.txtR5Letter1),
                    findViewById(R.id.txtR5Letter2),
                    findViewById(R.id.txtR5Letter3),
                    findViewById(R.id.txtR5Letter4),
                    findViewById(R.id.txtR5Letter5)
                )
                5 -> arrayOf(
                    findViewById(R.id.txtR6Letter1),
                    findViewById(R.id.txtR6Letter2),
                    findViewById(R.id.txtR6Letter3),
                    findViewById(R.id.txtR6Letter4),
                    findViewById(R.id.txtR6Letter5)
                )
                else -> arrayOf()
            }
        }

        // Set up text change listeners to handle user input
        for (row in editTexts.indices) {
            for (col in editTexts[row].indices) {
                val editText = editTexts[row][col]
                val nextEditText = if (col < editTexts[row].size - 1) editTexts[row][col + 1] else null
                editText.addTextChangedListener(GuessTextWatcher(editText, nextEditText))
            }
        }

        // Set focus on the first EditText
        editTexts[0][0].requestFocus()
    }

    private fun getCurrentGuess(): String {
        // Get the current guess entered by the user
        return editTexts[0].joinToString("") { it.text.toString().trim() }
    }

    private fun checkWord(guess: String) {
        // Check the user's guess against the correct word
        val requestBody = mapOf("guessedWord" to guess, "correctWord" to currentWord)
        wordleApiService.checkWord(requestBody).enqueue(object : Callback<WordValidationResponse> {
            override fun onResponse(call: Call<WordValidationResponse>, response: Response<WordValidationResponse>) {
                response.body()?.let { validationResult ->
                    runOnUiThread {
                        for ((index, result) in validationResult.results.withIndex()) {
                            val editText = editTexts[currentAttempt - 1][index]
                            val color = when (result.status) {
                                "correct" -> Color.GREEN
                                "present" -> Color.YELLOW
                                else -> Color.LTGRAY
                            }
                            editText.setBackgroundColor(color)
                        }
                    }
                    if (validationResult.results.all { it.status == "correct" }) {
                        Toast.makeText(this@MainActivity, "Correct!", Toast.LENGTH_SHORT).show()
                        // Handle win condition
                    } else if (currentAttempt < 6) {
                        handleNextGuessPreparation()
                        currentAttempt++
                    } else {
                        // Handle game over condition
                    }
                }
            }

            override fun onFailure(call: Call<WordValidationResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleNextGuessPreparation() {
        // Shift guesses down by one row for the next attempt
        for (row in 5 downTo 1) {
            for (col in 0 until 5) {
                editTexts[row][col].setText(editTexts[row - 1][col].text.toString())
                editTexts[row][col].background = editTexts[row - 1][col].background
            }
        }

        // Clear the first row for the next attempt
        for (editText in editTexts[0]) {
            editText.text.clear()
            editText.setBackgroundColor(Color.LTGRAY) // Set to gray anticipating the next guess
        }
    }

    private fun loadCorrectWord() {
        // Load the correct word from the API
        wordleApiService.generateWord().enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    currentWord = response.body()?.get("word") ?: ""
                    findViewById<TextView>(R.id.txtCorrectWord).text = currentWord
                } else {
                    Toast.makeText(this@MainActivity, "Failed to load correct word", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class GuessTextWatcher(private val currentEditText: EditText, private val nextEditText: EditText?) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (s != null && s.length == 1 && nextEditText != null) {
                nextEditText.requestFocus()
            }
        }
    }

    private interface WordleApiService {
        @GET("wordle/generateWord")
        fun generateWord(): Call<Map<String, String>>

        @POST("wordle/checkWord")
        fun checkWord(@Body request: Map<String, String>): Call<WordValidationResponse>
    }

    data class WordValidationResponse(
        val results: List<LetterResult>
    )

    data class LetterResult(
        val letter: Char,
        val status: String // "correct", "present", "absent"
    )
}