package com.example.pokemonlab.controller;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.pokemonlab.entity.Pokemon;
import com.example.pokemonlab.repository.PokemonRepository;

@Controller
public class HomeController {

    private final PokemonRepository pokemonRepository;

    public HomeController(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    @GetMapping("/")
    public String home(Model model) {

        // Pokemonテーブルからポケモンのデータを全権取得
        List<Pokemon> pokemonList = pokemonRepository.findAll();

        // ランダムで1匹取得
        Random random = new Random();

        Pokemon pokemon =
                pokemonList.get(random.nextInt(pokemonList.size()));

        model.addAttribute("pokemon", pokemon);

        return "plabhome";
    }

}