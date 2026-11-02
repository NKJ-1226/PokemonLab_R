package com.example.pokemonlab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pokemonlab.entity.PokemonEvolution;

public interface PokemonEvolutionRepository
        extends JpaRepository<PokemonEvolution, Integer> {

    // 指定ポケモンの進化先を取得
    List<PokemonEvolution> findByFromPokemonId(Integer fromPokemonId);

    // 指定ポケモンの進化前を取得
    List<PokemonEvolution> findByToPokemonId(Integer toPokemonId);
}