package com.example.pokemonlab.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pokemonlab.entity.Pokemon;

public interface PokemonRepository
        extends JpaRepository<Pokemon, Integer> {

}