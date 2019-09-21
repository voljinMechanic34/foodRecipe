package com.codingwithmitch.foodrecipes.requests.responses;

public class CheckRecipeApiKey {
        protected static boolean isRecipeApiKeyValid(RecipeSearchResponse recipeSearchResponse){
            return recipeSearchResponse.getError() == null;
        }
        protected  static boolean isRecipeApiKeyValid(RecipeResponse recipeResponse){
            return recipeResponse.getError() == null;
        }
}
