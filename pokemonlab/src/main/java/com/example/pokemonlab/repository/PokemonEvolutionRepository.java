
package com.example.pokemonlab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.pokemonlab.entity.PokemonEvolution;

/**
 * ポケモンの進化関係を取得するRepository
 *
 * 進化前・進化先のIDを条件として、
 * pokemon_evolutionテーブルを検索する
 */
public interface PokemonEvolutionRepository
        extends JpaRepository<PokemonEvolution, Integer> {

    /**
     * 指定したポケモンから進化する先を検索する
     *
     * @param fromPokemonId 進化前ポケモンのID
     * @return 進化先への関係一覧
     */
    List<PokemonEvolution> findByFromPokemonId(Integer fromPokemonId);

    /**
     * 指定したポケモンへの進化元を検索する
     *
     * @param toPokemonId 進化先ポケモンのID
     * @return 進化元との関係一覧
     */
    List<PokemonEvolution> findByToPokemonId(Integer toPokemonId);
}
