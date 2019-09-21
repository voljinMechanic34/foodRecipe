package com.codingwithmitch.foodrecipes.requests.responses;

import com.codingwithmitch.foodrecipes.models.Recipe;

import java.io.IOException;

import retrofit2.Response;

public class ApiResponse<T> {

    public ApiResponse<T> create(Throwable error){
        return new ApiErrorResponse<>(!error.getMessage().equals("") ? error.getMessage() : "Unknown error\n Check network connection");
        
    }
    public ApiResponse<T> create (Response<T> response){
        if (response.isSuccessful()){
            T body = response.body();
            if (body instanceof RecipeSearchResponse){
                if (!CheckRecipeApiKey.isRecipeApiKeyValid((RecipeSearchResponse)body)){
                    String errorMsg = "Api key is invalid or expired.";
                    return new ApiErrorResponse<>(errorMsg);
                }
            }
            if (body instanceof RecipeResponse){
                if (!CheckRecipeApiKey.isRecipeApiKeyValid((RecipeResponse)body)){
                    String errorMsg = "Api key is invalid or expired.";
                    return new ApiErrorResponse<>(errorMsg);
                }
            }
            if (body == null  || response.code() == 204){
                //empty response code
                return new ApiEmptyResponse<>();
            }    else  {
                return new ApiSuccessResponse<>(body);
            }
        }   else {
            String errorMsg = "";
            try {
                errorMsg = response.errorBody().string();

            }   catch (IOException e){
                   e.printStackTrace();
                   errorMsg = response.message();
            }
            return new ApiErrorResponse<>(errorMsg);
        }
    }
    public class ApiSuccessResponse<T> extends ApiResponse<T> {
        private T body;

        public ApiSuccessResponse(T body) {
            this.body = body;
        }
        public T getBody(){
            return  body;
        }
    }
    public class ApiErrorResponse<T> extends ApiResponse<T> {
            private String errorMessage;

        public ApiErrorResponse(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        public String getErrorMessage(){
            return errorMessage;
        }
    }
    public class ApiEmptyResponse<T> extends ApiResponse<T> {
        
    }
}
