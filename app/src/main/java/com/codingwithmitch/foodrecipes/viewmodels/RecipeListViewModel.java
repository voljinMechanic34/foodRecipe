package com.codingwithmitch.foodrecipes.viewmodels;


import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.repositories.RecipeRepository;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {

    private static final String TAG = "RecipeListViewModel";

    public static final String QUERY_EXHAUSTED = "No more results";
    public enum ViewState {CATEGORIES, RECIPES}

    private MutableLiveData<ViewState> viewState;
    private MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();

    private RecipeRepository recipeRepository;

    //query extras
    private boolean isQueryExhausted;
    private boolean isPerformingQuery;
    private int pagenumber;
    private String query;
    private boolean cancelRequest;
    private long  requestStartTime;
    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = RecipeRepository.getInstance(application);
        init();
    }

    private void init(){
        if(viewState == null){
            viewState = new MutableLiveData<>();
            viewState.setValue(ViewState.CATEGORIES);
        }
    }
    public LiveData<ViewState> getViewstate(){
        return viewState;
    }

    public void searchNextPage(){
        if (!isQueryExhausted && !isPerformingQuery){
            pagenumber++;
            executeSearch();
        }
    }
    public LiveData<Resource<List<Recipe>>> getRecipes(){
        return recipes;
    }
    public int getPageNumber(){
        return pagenumber;
    }
    public void setViewCategories(){
        viewState.setValue(ViewState.CATEGORIES);
    }
    public void searchRecipesApi(String query, int pagenumber){
        if (!isPerformingQuery){
            if (pagenumber == 0){
                pagenumber = 1;
            }
            this.pagenumber = pagenumber;
            this.query = query;
            isQueryExhausted = false;
            executeSearch();
        }
    }

    private void executeSearch(){
        requestStartTime = System.currentTimeMillis();
        cancelRequest = false;
        isPerformingQuery = true;
        viewState.setValue(ViewState.RECIPES);
        final LiveData<Resource<List<Recipe>>> repositorySource = recipeRepository.searchRecipesApi(query,pagenumber);

        recipes.addSource(repositorySource, new Observer<Resource<List<Recipe>>>() {
            @Override
            public void onChanged(@Nullable Resource<List<Recipe>> listResource) {
                if (!cancelRequest){
                    if (listResource != null){
                        recipes.setValue(listResource);
                        if (listResource.status == Resource.Status.SUCCESS){
                            Log.d(TAG, "onChanged: Request time : " +(System.currentTimeMillis()-requestStartTime)/1000+"seconds");
                            isPerformingQuery = false;
                            if (listResource.data != null){
                                if (listResource.data.size() == 0){
                                    Log.d(TAG, "onChanged: query is exhausted..");
                                    recipes.setValue(
                                            new Resource<List<Recipe>>(
                                                    Resource.Status.ERROR,
                                                    listResource.data,
                                                    QUERY_EXHAUSTED

                                            )
                                    );
                                }
                            }
                            recipes.removeSource(repositorySource);
                        } else if (listResource.status == Resource.Status.ERROR){
                            isPerformingQuery = false;
                            recipes.removeSource(repositorySource);
                        }
                    }else {
                        recipes.removeSource(repositorySource);
                    }
                } else{
                    recipes.removeSource(repositorySource);
                }

            }
        });
    }
    public void cancelSearchRequest(){
        if (isPerformingQuery){
            Log.d(TAG, "cancelSearchRequest: canceling the search request");
            cancelRequest = true;
            isPerformingQuery = false;
            pagenumber = 1;

        }
    }
}















