package com.example.pokemonlab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.pokemonlab.entity.Pokemon;
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

    @GetMapping("/pokedex/{id}")
    public String detail(
            @PathVariable Integer id,
            Model model) {

        Pokemon pokemon = pokemonRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "指定されたポケモンが見つかりません。id=" + id
                        )
                );

        model.addAttribute("pokemon", pokemon);

        pokemonRepository
                .findFirstByIdLessThanOrderByIdDesc(id)
                .ifPresent(previous ->
                        model.addAttribute("previousPokemon", previous)
                );

        pokemonRepository
                .findFirstByIdGreaterThanOrderByIdAsc(id)
                .ifPresent(next ->
                        model.addAttribute("nextPokemon", next)
                );

        return "pokedex-detail";
    }
}