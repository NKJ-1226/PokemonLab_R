package com.example.pokemonlab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.pokemonlab.repository.PokemonRepository;

@Controller
public class PokedexController {

    private final PokemonRepository pokemonRepository;

    public PokedexController(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    @GetMapping("/pokedex")
    public String pokedex(Model model) {

        model.addAttribute(
            "pokemonList",
            pokemonRepository.findAll()
        );

        return "pokedex";
    }
}