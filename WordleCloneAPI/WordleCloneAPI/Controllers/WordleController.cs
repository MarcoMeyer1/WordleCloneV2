using Microsoft.AspNetCore.Mvc;
using System;
using System.Collections.Generic;

namespace WordleCloneAPI.Controllers
{
    /// <summary>
    /// Controller for Wordle game functionalities.
    /// </summary>
    [Route("api/[controller]")]
    [ApiController]
    public class WordleController : ControllerBase
    {
        // Predefined list of words for the game
        private static readonly List<string> WordList = new List<string> {
            "apple", "beach", "cloud", "dance", "eagle",
            "fruit", "grape", "house", "juice", "knife",
            "lemon", "music", "night", "ocean", "pizza",
            "queen", "river", "sunny", "teeth", "uncle",
            "vodka", "water", "yacht", "zebra"
        };

        /// <summary>
        /// Generates a random word from the predefined list.
        /// </summary>
        /// <returns>The generated word.</returns>
        [HttpGet("generateWord")]
        [ProducesResponseType(typeof(string), 200)]
        public ActionResult<string> GenerateWord()
        {
            Random random = new Random();
            int index = random.Next(WordList.Count);
            return Ok(new { word = WordList[index] });
        }

        /// <summary>
        /// Checks if a word is valid according to the game's rules.
        /// </summary>
        /// <param name="request">The word validation request containing guessed and correct words.</param>
        /// <returns>Feedback indicating whether the word is correct or not.</returns>
        [HttpPost("checkWord")]
        public ActionResult CheckWord([FromBody] WordValidationRequest request)
        {
            var response = new WordValidationResponse
            {
                Results = new List<LetterResult>()
            };

            if (string.IsNullOrEmpty(request.GuessedWord) || string.IsNullOrEmpty(request.CorrectWord))
            {
                return BadRequest("Invalid input. Please provide both guessed and correct words.");
            }

            var guessedWord = request.GuessedWord.ToLower();
            var correctWord = request.CorrectWord.ToLower();
            var letterHasBeenUsed = new bool[correctWord.Length];

            // Loop through each letter in the guessed word
            for (int i = 0; i < guessedWord.Length; i++)
            {
                var letterResult = new LetterResult
                {
                    Letter = guessedWord[i],
                    Status = "absent"
                };

                // Check if the letter is in the correct position
                if (guessedWord[i] == correctWord[i])
                {
                    letterResult.Status = "correct";
                    letterHasBeenUsed[i] = true;
                }
                // Check if the letter is present but in the wrong position
                else if (correctWord.Contains(guessedWord[i]))
                {
                    var foundAtIndex = correctWord.IndexOf(guessedWord[i]);
                    if (!letterHasBeenUsed[foundAtIndex])
                    {
                        letterResult.Status = "present";
                        letterHasBeenUsed[foundAtIndex] = true;
                    }
                }

                response.Results.Add(letterResult);
            }

            return Ok(response);
        }

        // Class for deserializing word validation requests
        public class WordValidationRequest
        {
            public string GuessedWord { get; set; }
            public string CorrectWord { get; set; }
        }

        // Class for serializing word validation responses
        public class WordValidationResponse
        {
            public List<LetterResult> Results { get; set; }
        }

        // Class representing the status of each letter in the word
        public class LetterResult
        {
            public char Letter { get; set; }
            public string Status { get; set; } // Possible values: "correct", "present", "absent"
        }

    }
}