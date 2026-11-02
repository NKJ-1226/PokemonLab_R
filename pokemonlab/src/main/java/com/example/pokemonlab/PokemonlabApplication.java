
package com.example.pokemonlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PokemonLab_Rの起動クラス
 *
 * Spring Bootアプリケーションのエントリーポイント
 */
@SpringBootApplication
public class PokemonlabApplication {

    /**
     * アプリケーションを起動する
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(PokemonlabApplication.class, args);
    }
}
