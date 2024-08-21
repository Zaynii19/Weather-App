package com.example.weatherapp

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        customizeSearchView()

        fetchWeatherData("islamabad")
        setupSearchListener()

        binding.refreshBtn.setOnClickListener {
            refreshWeatherData()
            Toast.makeText(this, "Weather Data Refreshed Successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun customizeSearchView() {
        val searchEditText = binding.search.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.BLACK)
        searchEditText.setHintTextColor(Color.GRAY)
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(ApiInterface::class.java)

        val response = retrofit.getWeatherData(cityName, "YourApikey", "metric")
        Log.d("Retrofit URL", response.request().url().toString())

        response.enqueue(object : Callback<WeatherData> {
            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        Log.d("WeatherData", "Response: $responseBody")
                        updateUIWithWeatherData(cityName, responseBody)
                    } else {
                        Log.e("WeatherData", "Response body is null")
                    }
                } else {
                    Log.e("WeatherData", "Request failed with code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                Log.e("WeatherData", "Network request failed: ${t.message}")
            }
        })
    }

    private fun updateUIWithWeatherData(cityName: String, weatherData: WeatherData) {
        val temperature = weatherData.main.temp.toInt()
        val minTemperature = weatherData.main.temp_min.toInt()
        val maxTemperature = weatherData.main.temp_max.toInt()
        val feelLike = weatherData.main.feels_like.toInt()
        val humidity = weatherData.main.humidity
        val windSpeed = weatherData.wind.speed
        val sunriseTime = convertUnixToTime(weatherData.sys.sunrise)
        val sunsetTime = convertUnixToTime(weatherData.sys.sunset)
        val visibilityInKilometers = weatherData.visibility / 1000
        val seaLevel = weatherData.main.sea_level
        val condition = weatherData.weather.firstOrNull()?.main ?: "Unknown"

        updateBackgroundAndIcon(condition)
        binding.temperature.text = temperature.toString()
        binding.minTemp.text = minTemperature.toString()
        binding.maxTemp.text = maxTemperature.toString()
        binding.feelLike.text = buildString {
            append("Feel like: ")
            append(feelLike)
        }
        binding.humidity.text = buildString {
            append(humidity)
            append("%")
        }
        binding.wind.text = buildString {
            append(windSpeed)
            append(" m/s")
        }
        binding.sea.text = buildString {
            append(seaLevel)
            append(" hPa")
        }
        binding.sunrise.text = sunriseTime
        binding.sunset.text = sunsetTime
        binding.visibility.text = buildString {
            append(visibilityInKilometers)
            append(" km")
        }
        binding.weatherConditionText.text = condition
        binding.locationName.text = cityName
    }

    private fun updateBackgroundAndIcon(condition: String) {
        val categorizedCondition = categorizeWeatherCondition(condition)
        when (categorizedCondition) {
            "Sunny" -> {
                binding.main.setBackgroundResource(R.drawable.sunny_background)
                binding.weatherConditionPic.setImageResource(R.drawable.sun)
            }
            "Cloudy" -> {
                binding.main.setBackgroundResource(R.drawable.cloud_background)
                binding.weatherConditionPic.setImageResource(R.drawable.cloudy)
            }
            "Rainy" -> {
                binding.main.setBackgroundResource(R.drawable.rain_background)
                binding.weatherConditionPic.setImageResource(R.drawable.rainy)
            }
            "Snow" -> {
                binding.main.setBackgroundResource(R.drawable.snow_background)
                binding.weatherConditionPic.setImageResource(R.drawable.snowy)
            }
            else -> {
                binding.main.setBackgroundResource(R.drawable.sunny_background)
                binding.weatherConditionPic.setImageResource(R.drawable.sun)
            }
        }
        binding.weatherConditionText.text = categorizedCondition
    }

    private fun categorizeWeatherCondition(condition: String): String {
        return when (condition) {
            "Clear" -> "Sunny"
            "Clouds", "Mist", "Haze", "Smoke", "Fog", "Dust", "Sand", "Tornado" -> "Cloudy"
            "Rain", "Drizzle", "Thunderstorm", "Squall" -> "Rainy"
            "Snow", "Ash" -> "Snow"
            else -> "Unknown"
        }
    }

    private fun setupSearchListener() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchWeatherData(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun convertUnixToTime(unixTime: Int): String {
        val date = Date(unixTime * 1000L)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    private fun refreshWeatherData() {
        val cityName = binding.locationName.text.toString()
        fetchWeatherData(cityName)
    }
}
