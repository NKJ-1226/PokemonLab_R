package com.example.pokemonlab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.pokemonlab.entity.Pokemon;

public interface PokemonRepository
        extends JpaRepository<Pokemon, Integer> {

    Optional<Pokemon> findFirstByIdLessThanOrderByIdDesc(Integer id);

    Optional<Pokemon> findFirstByIdGreaterThanOrderByIdAsc(Integer id);

    @Query(value = "SELECT * FROM pokemon ORDER BY RAND() LIMIT 4",
           nativeQuery = true)
                List<Pokemon> findRandomFour();

        List<Pokemon> findByGenerationAndEvolvesToIsNull(
                Integer generation
        );

        List<Pokemon> findByEvolvesToIsNull();
}