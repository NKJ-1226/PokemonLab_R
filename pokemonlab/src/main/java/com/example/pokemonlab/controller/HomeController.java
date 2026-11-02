
package com.example.pokemonlab.controller;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.pokemonlab.entity.Pokemon;
import com.example.pokemonlab.repository.PokemonRepository;

/**
 * ホーム画面を表示するController
 *
 * DBに登録されたポケモンからランダムに1匹を選び、ホーム画面に表示
 */
@Controller
public class HomeController {

    private final PokemonRepository pokemonRepository;

    public HomeController(PokemonRepository pokemonRepository) {
        this.pokemonRepository = pokemonRepository;
    }

    /**
     * ホーム画面を表示する
     *
     * @param model 画面へ渡すデータを保持するModel
     * @return ホーム画面のテンプレート名
     */
    @GetMapping("/")
    public String home(Model model) {

        // DBからポケモンを全件取得
        List<Pokemon> pokemonList = pokemonRepository.findAll();

        // ランダムに表示するポケモンを1匹選ぶ
        Random random = new Random();

        Pokemon pokemon = pokemonList.get(
                random.nextInt(pokemonList.size()));

        // 選択したポケモンを画面へ渡す
        model.addAttribute("pokemon", pokemon);

        return "plabhome";
    }
}
