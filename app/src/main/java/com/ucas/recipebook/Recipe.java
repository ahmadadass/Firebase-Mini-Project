package com.ucas.recipebook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Recipe {
    private String id;
    private String name;
    private String description;
    private String ingredients;
    private String steps;
    private String category;
    private String videoUrl;
    private String base64Image;

    private String authorId;
    private String authorName;
    private boolean publicRecipe;

    private List<String> likedBy;
    private Date createdAt;

    // Required empty constructor for Firestore
    public Recipe() {
        likedBy = new ArrayList<>();
    }

    public Recipe(String id, String name, String description, String ingredients,
                  String steps, String category, String videoUrl, String base64Image,
                  String authorId, String authorName, boolean publicRecipe,
                  List<String> likedBy, Date createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ingredients = ingredients;
        this.steps = steps;
        this.category = category;
        this.videoUrl = videoUrl;
        this.base64Image = base64Image;
        this.authorId = authorId;
        this.authorName = authorName;
        this.publicRecipe = publicRecipe;
        this.likedBy = likedBy;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIngredients() { return ingredients; }
    public String getSteps() { return steps; }
    public String getCategory() { return category; }
    public String getVideoUrl() { return videoUrl; }
    public String getBase64Image() { return base64Image; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public boolean isPublicRecipe() { return publicRecipe; }
    public List<String> getLikedBy() { return likedBy; }
    public Date getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setCategory(String category) { this.category = category; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setBase64Image(String base64Image) { this.base64Image = base64Image; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setPublicRecipe(boolean publicRecipe) { this.publicRecipe = publicRecipe; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}