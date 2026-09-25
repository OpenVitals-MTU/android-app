package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.model.CustomFood

/** The user's food catalog. A logged portion is written through [NutritionRepository], not here. */
interface FoodRepository {
    suspend fun customFoods(): List<CustomFood>

    suspend fun saveCustomFood(food: CustomFood)

    suspend fun deleteCustomFood(foodId: String)
}
