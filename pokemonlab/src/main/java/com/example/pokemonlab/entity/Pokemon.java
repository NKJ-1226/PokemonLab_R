package com.example.pokemonlab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pokemon")
public class Pokemon {

    @Id
    private Integer id;

    @Column(name = "pokedex_no")
    private Integer pokedexNo;

    // 世代カラムを追加
    @Column(name = "generation")
    private Integer generation;

    private String name;

    @Column(name = "type_1")
    private String type1;

    @Column(name = "type_2")
    private String type2;

    @Column(name = "evolves_from")
    private Integer evolvesFrom;

    @Column(name = "evolves_to")
    private Integer evolvesTo;

    private Integer hp;

    private Integer attack;

    private Integer defense;

    @Column(name = "sp_attack")
    private Integer spAttack;

    @Column(name = "sp_defense")
    private Integer spDefense;

    private Integer speed;

    private Integer total;
}