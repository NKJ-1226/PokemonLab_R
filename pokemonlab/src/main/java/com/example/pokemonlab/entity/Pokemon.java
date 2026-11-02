package com.example.pokemonlab.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pokemon")
public class Pokemon {

    // キー項目(ID)
    @Id
    private Integer id;

    // 図鑑No
    @Column(name = "pokedex_no")
    private Integer pokedexNo;

    // 世代
    @Column(name = "generation")
    private Integer generation;

    // 名前
    private String name;

    // タイプ1
    @Column(name = "type_1")
    private String type1;

    // タイプ2
    @Column(name = "type_2")
    private String type2;

    // 進化前ID
    @Column(name = "evolves_from")
    private Integer evolvesFrom;

    // 進化先ID
    @Column(name = "evolves_to")
    private Integer evolvesTo;

    // HP種族値
    private Integer hp;

    //　攻撃種族値
    private Integer attack;

    //　防御種族値
    private Integer defense;

    //　特攻種族値
    @Column(name = "sp_attack")
    private Integer spAttack;

    //　特防種族値
    @Column(name = "sp_defense")
    private Integer spDefense;

    //　素早さ種族値
    private Integer speed;

    //　合計種族値
    private Integer total;

    //　分類
    @Column(name = "category")
    private String category;

    // 高さ
    @Column(name = "height")
    private BigDecimal height;

    // 重さ
    @Column(name = "weight")
    private BigDecimal weight;
}