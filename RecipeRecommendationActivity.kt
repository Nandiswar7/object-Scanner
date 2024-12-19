package com.example.objectscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.IOException

class RecipeRecommendationActivity : AppCompatActivity() {

    private lateinit var recommendationsTextView: TextView
    private lateinit var webView: WebView
    private lateinit var goBackBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_recommendation)

        recommendationsTextView = findViewById(R.id.recommendationsTextView)
        webView = findViewById(R.id.webView)
        goBackBtn = findViewById(R.id.goBackBtn)

        val detectedIngredients = intent.getStringArrayListExtra("detectedIngredients") ?: emptyList()

        if (detectedIngredients.isEmpty()) {
            recommendationsTextView.text = "No ingredients detected."
        } else {
            recommendRecipes(detectedIngredients)
        }

        goBackBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun recommendRecipes(detectedIngredients: List<String>) {
        val recipes = parseRecipesFromJson()
        val matchedRecipes = recipes.map { recipe ->
            val matchedIngredients = recipe.ingredients.filter { it in detectedIngredients }
            val missingIngredients = recipe.ingredients.filterNot { it in detectedIngredients }
            RecipeWithDetails(recipe.name, recipe.link, matchedIngredients, missingIngredients)
        }.filter { it.matchedIngredients.isNotEmpty() } // Only include recipes with at least one matched ingredient

        if (matchedRecipes.isNotEmpty()) {
            val recommendationsHtml = matchedRecipes
                .sortedByDescending { it.matchedIngredients.size } // Sort by highest matching ingredients first
                .joinToString("<br><br>") { recipe ->
                    val zeptoLinks = recipe.missingIngredients.joinToString("<br>") { ingredient ->
                        val link = generateZeptoLink(ingredient)
                        "<a href='$link'>Zepto Link for ${ingredient}</a>"
                    }

                    """
                    <b>${recipe.name}:</b><br>
                    <a href="${recipe.link}">${recipe.link}</a><br>
                    Missing Ingredients: ${recipe.missingIngredients.joinToString(", ")}<br>
                    $zeptoLinks
                    """.trimIndent()
                }

            webView.loadDataWithBaseURL(null, recommendationsHtml, "text/html; charset=UTF-8", "UTF-8", null)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = CustomWebViewClient()
            webView.isScrollbarFadingEnabled = true // Makes the scrollbar visible
        } else {
            recommendationsTextView.text = "No recipes found for the detected ingredients."
        }
    }

    private fun parseRecipesFromJson(): List<Recipe> {
        return try {
            val inputStream = assets.open("dataset.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            gson.fromJson(json, Array<Recipe>::class.java).toList()
        } catch (e: IOException) {
            recommendationsTextView.text = "Error reading recipe file: ${e.message}"
            emptyList()
        }
    }

    private fun generateZeptoLink(ingredient: String): String {
        // Replace with a function that creates a valid Zepto link based on the ingredient name or ID
        return "https://www.zeptonow.com/search?query=$ingredient"
    }

    data class Recipe(
        val name: String,
        val ingredients: List<String>,
        val link: String
    )

    data class RecipeWithDetails(
        val name: String,
        val link: String,
        val matchedIngredients: List<String>,
        val missingIngredients: List<String>
    )

    private inner class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }
            return false
        }
    }
}
