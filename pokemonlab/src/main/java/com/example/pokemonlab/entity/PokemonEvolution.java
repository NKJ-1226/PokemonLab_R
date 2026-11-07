
package com.example.pokemonlab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * ポケモンの進化関係を表すEntity
 * 進化前と進化先のポケモンIDを管理する
 */
@Data
@Entity
@Table(name = "pokemon_evolution")
public class PokemonEvolution {

    /** 進化関係を識別する主キー */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 進化前のポケモンID */
    @Column(name = "from_pokemon_id", nullable = false)
    private Integer fromPokemonId;

    /** 進化先のポケモンID */
    @Column(name = "to_pokemon_id", nullable = false)
    private Integer toPokemonId;
}
