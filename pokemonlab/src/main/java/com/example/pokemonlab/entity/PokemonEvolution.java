package com.example.pokemonlab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "pokemon_evolution")
public class PokemonEvolution {

    // キー項目(ID)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 進化前ID
    @Column(name = "from_pokemon_id", nullable = false)
    private Integer fromPokemonId;

    // 進化先ID
    @Column(name = "to_pokemon_id", nullable = false)
    private Integer toPokemonId;
}