package com.example.pokemonlab.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pokemonlab.entity.Pokemon;

public interface PokemonRepository
        extends JpaRepository<Pokemon, Integer> {

    Optional<Pokemon> findFirstByIdLessThanOrderByIdDesc(Integer id);

    Optional<Pokemon> findFirstByIdGreaterThanOrderByIdAsc(Integer id);

}