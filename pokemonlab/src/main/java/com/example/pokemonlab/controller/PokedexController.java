package com.example.pokemonlab.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.pokemonlab.entity.Pokemon;
import com.example.pokemonlab.entity.PokemonEvolution;
import com.example.pokemonlab.repository.PokemonRepository;
import com.example.pokemonlab.repository.PokemonEvolutionRepository;

import jakarta.persistence.criteria.Predicate;

@Controller
public class PokedexController {

    /**
     * 1ページに表示するポケモン数
     */
    private static final int PAGE_SIZE = 20;

    /**
     * 画面に同時表示するページ番号の最大数
     */
    private static final int PAGE_NUMBER_DISPLAY_COUNT = 5;

    /**
     * タイプ検索の選択肢
     *
     * DBに保存しているタイプ名と
     * 同じ表記に合わせる
     */
    private static final List<String> POKEMON_TYPES = List.of(
            "ノーマル",
            "ほのお",
            "みず",
            "でんき",
            "くさ",
            "こおり",
            "かくとう",
            "どく",
            "じめん",
            "ひこう",
            "エスパー",
            "むし",
            "いわ",
            "ゴースト",
            "ドラゴン",
            "あく",
            "はがね",
            "フェアリー"
    );

    private final PokemonRepository pokemonRepository;
    private final PokemonEvolutionRepository pokemonEvolutionRepository;

    public PokedexController(
            PokemonRepository pokemonRepository,
            PokemonEvolutionRepository pokemonEvolutionRepository) {
        this.pokemonRepository = pokemonRepository;
        this.pokemonEvolutionRepository = pokemonEvolutionRepository;
    }
    

    /**
     * ポケモン図鑑一覧を表示する
     *
     * 使用例：
     *
     * /pokedex
     * /pokedex?name=ピカ
     * /pokedex?generation=1
     * /pokedex?type=でんき
     * /pokedex?name=ピカ&generation=1&type=でんき&page=0
     */
    @GetMapping("/pokedex")
    public String pokedex(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Integer generation,
            @RequestParam(defaultValue = "") String type,
            Model model) {

        /*
         * 前後の空白を除去する
         */
        String normalizedName = normalizeText(name);
        String normalizedType = normalizeText(type);

        /*
         * URLからマイナスのページ番号が渡された場合は
         * 0ページ目へ補正する
         */
        int requestedPage = Math.max(page, 0);

        /*
         * 検索条件を動的に作成する
         */
        Specification<Pokemon> specification =
                createSearchSpecification(
                        normalizedName,
                        generation,
                        normalizedType
                );

        /*
         * 図鑑Noの昇順で表示する
         * 同じ図鑑Noが存在した場合はidの昇順にする
         */
        Sort sort = Sort.by(
                Sort.Order.asc("pokedexNo"),
                Sort.Order.asc("id")
        );

        Pageable pageable = PageRequest.of(
                requestedPage,
                PAGE_SIZE,
                sort
        );

        Page<Pokemon> pokemonPage =
                pokemonRepository.findAll(
                        specification,
                        pageable
                );

        /*
         * 検索条件を変えた後などに、
         * 存在しないページ番号が指定された場合の補正
         *
         * 例：
         * 全件表示中に50ページ目を見ていた
         *      ↓
         * 第1世代へ絞り込んだ
         *      ↓
         * 50ページ目が存在しなくなる
         *
         * この場合は絞り込み結果の最終ページを表示する
         */
        if (pokemonPage.getTotalPages() > 0
                && requestedPage >= pokemonPage.getTotalPages()) {

            requestedPage = pokemonPage.getTotalPages() - 1;

            pageable = PageRequest.of(
                    requestedPage,
                    PAGE_SIZE,
                    sort
            );

            pokemonPage = pokemonRepository.findAll(
                    specification,
                    pageable
            );
        }

        int currentPage = pokemonPage.getNumber();

        List<Integer> pageNumbers = createPageNumbers(
                currentPage,
                pokemonPage.getTotalPages()
        );

        long startItem = pokemonPage.isEmpty()
                ? 0
                : pokemonPage.getPageable().getOffset() + 1;

        long endItem = pokemonPage.isEmpty()
                ? 0
                : startItem
                    + pokemonPage.getNumberOfElements()
                    - 1;

        boolean searchActive =
                StringUtils.hasText(normalizedName)
                || generation != null
                || StringUtils.hasText(normalizedType);

        model.addAttribute(
                "pokemonList",
                pokemonPage.getContent()
        );

        model.addAttribute(
                "pokemonPage",
                pokemonPage
        );

        model.addAttribute(
                "pageNumbers",
                pageNumbers
        );

        model.addAttribute(
                "currentPage",
                currentPage
        );

        model.addAttribute(
                "totalElements",
                pokemonPage.getTotalElements()
        );

        model.addAttribute(
                "startItem",
                startItem
        );

        model.addAttribute(
                "endItem",
                endItem
        );

        /*
         * 検索フォームへ入力内容を戻すための値
         */
        model.addAttribute(
                "searchName",
                normalizedName
        );

        model.addAttribute(
                "selectedGeneration",
                generation
        );

        model.addAttribute(
                "selectedType",
                normalizedType
        );

        model.addAttribute(
                "pokemonTypes",
                POKEMON_TYPES
        );

        model.addAttribute(
                "searchActive",
                searchActive
        );

        return "pokedex";
    }

    /**
     * 名前・世代・タイプの検索条件を作成する
     */
    private Specification<Pokemon> createSearchSpecification(
            String name,
            Integer generation,
            String type) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            /*
             * 名前の部分一致検索
             *
             * 例：
             * 「ピカ」→「ピカチュウ」
             */
            if (StringUtils.hasText(name)) {

                String escapedName =
                        escapeLikePattern(
                                name.toLowerCase()
                        );

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("name")
                                ),
                                "%" + escapedName + "%",
                                '\\'
                        )
                );
            }

            /*
             * 世代の完全一致検索
             */
            if (generation != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("generation"),
                                generation
                        )
                );
            }

            /*
             * タイプ1またはタイプ2の
             * どちらかに一致すれば対象とする
             */
            if (StringUtils.hasText(type)) {

                Predicate type1Matches =
                        criteriaBuilder.equal(
                                root.get("type1"),
                                type
                        );

                Predicate type2Matches =
                        criteriaBuilder.equal(
                                root.get("type2"),
                                type
                        );

                predicates.add(
                        criteriaBuilder.or(
                                type1Matches,
                                type2Matches
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }

    /**
     * エスケープ処理
     *
     * %  任意の文字列
     * _  任意の1文字
     */
    private String escapeLikePattern(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * nullを空文字変換
     */
    private String normalizeText(String text) {

        if (text == null) {
            return "";
        }

        return text.trim();
    }

    /**
     * 画面に表示するページ番号を作成
     */
    private List<Integer> createPageNumbers(
            int currentPage,
            int totalPages) {

        if (totalPages <= 0) {
            return List.of();
        }

        int halfDisplayCount =
                PAGE_NUMBER_DISPLAY_COUNT / 2;

        int startPage = Math.max(
                0,
                currentPage - halfDisplayCount
        );

        int endPage = Math.min(
                totalPages - 1,
                startPage
                    + PAGE_NUMBER_DISPLAY_COUNT
                    - 1
        );

        /*
         * 最終ページ付近でも、
         * 可能な限り5ページ分表示
         */
        startPage = Math.max(
                0,
                endPage
                    - PAGE_NUMBER_DISPLAY_COUNT
                    + 1
        );

        return IntStream
                .rangeClosed(startPage, endPage)
                .boxed()
                .toList();
    }

    /**
     * ポケモン詳細画面を表示
     */
    @GetMapping("/pokedex/{id}")
    public String detail(
            @PathVariable Integer id,
            Model model) {

        Pokemon pokemon = pokemonRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "指定されたポケモンが見つかりませんid="
                                + id
                        )
                );

        model.addAttribute(
                "pokemon",
                pokemon
        );

        pokemonRepository
                .findFirstByIdLessThanOrderByIdDesc(id)
                .ifPresent(previous ->
                        model.addAttribute(
                                "previousPokemon",
                                previous
                        )
                );

        pokemonRepository
                .findFirstByIdGreaterThanOrderByIdAsc(id)
                .ifPresent(next ->
                        model.addAttribute(
                                "nextPokemon",
                                next
                        )
                );

        // 進化テーブルから進化前・進化先の情報を取得する
        List<PokemonEvolution> fromRelations =
                pokemonEvolutionRepository.findByToPokemonId(id);

        List<PokemonEvolution> toRelations =
                pokemonEvolutionRepository.findByFromPokemonId(id);

        // 進化前のポケモン情報（分岐進化対応）
        List<Pokemon> evolutionFromList = fromRelations.stream()
                .map(relation -> pokemonRepository
                        .findById(relation.getFromPokemonId())
                        .orElse(null))
                .filter(evolutionFrom -> evolutionFrom != null)
                .toList();

        // 進化先のポケモン情報（分岐進化対応）
        List<Pokemon> evolutionToList = toRelations.stream()
                .map(relation -> pokemonRepository
                        .findById(relation.getToPokemonId())
                        .orElse(null))
                .filter(evolutionTo -> evolutionTo != null)
                .toList();

        model.addAttribute("evolutionFromList", evolutionFromList);
        model.addAttribute("evolutionToList", evolutionToList);

        // 進化前・進化先をたどり、最終進化までの進化ラインを取得する
        List<List<Pokemon>> evolutionStages = createEvolutionStages(id);
        model.addAttribute("evolutionStages", evolutionStages);

        return "pokedex-detail";
    }

    /**
     * 進化ラインの作成
     * 分岐進化、2段階進化対応
     */
    private List<List<Pokemon>> createEvolutionStages(Integer pokemonId) {

        // 進化ラインを取得
        List<PokemonEvolution> relations = pokemonEvolutionRepository.findAll();
        Map<Integer, List<Integer>> nextMap = new HashMap<>();
        Map<Integer, List<Integer>> previousMap = new HashMap<>();

        for (PokemonEvolution relation : relations) {
            Integer fromId = relation.getFromPokemonId();
            Integer toId = relation.getToPokemonId();
            nextMap.computeIfAbsent(fromId, key -> new ArrayList<>()).add(toId);
            previousMap.computeIfAbsent(toId, key -> new ArrayList<>()).add(fromId);
        }

        // 進化前/後が登録されていない場合は空欄
        if (!nextMap.containsKey(pokemonId) && !previousMap.containsKey(pokemonId)) {
            return List.of();
        }

        // 進化前・進化先の両方向をたどり、同じ進化ラインに属するIDを取得
        Set<Integer> connectedIds = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        connectedIds.add(pokemonId);
        queue.add(pokemonId);

        while (!queue.isEmpty()) {
            Integer currentId = queue.removeFirst();
            List<Integer> neighbors = new ArrayList<>();
            neighbors.addAll(nextMap.getOrDefault(currentId, List.of()));
            neighbors.addAll(previousMap.getOrDefault(currentId, List.of()));

            for (Integer neighborId : neighbors) {
                if (connectedIds.add(neighborId)) {
                    queue.addLast(neighborId);
                }
            }
        }

        // たねポケモンから進化ラインを取得
        Map<Integer, Integer> stageById = new HashMap<>();
        queue.clear();

        for (Integer candidateId : connectedIds) {
            boolean hasPrevious = previousMap.getOrDefault(candidateId, List.of())
                    .stream().anyMatch(connectedIds::contains);
            if (!hasPrevious) {
                stageById.put(candidateId, 0);
                queue.addLast(candidateId);
            }
        }

        // 無限ループ回避
        if (queue.isEmpty()) {
            return List.of();
        }

        Map<Integer, Integer> indegrees = new HashMap<>();
        for (Integer candidateId : connectedIds) {
            int degree = (int) previousMap.getOrDefault(candidateId, List.of())
                    .stream().filter(connectedIds::contains).count();
            indegrees.put(candidateId, degree);
        }
        while (!queue.isEmpty()) {
            Integer currentId = queue.removeFirst();
            int currentStage = stageById.get(currentId);
            for (Integer nextId : nextMap.getOrDefault(currentId, List.of())) {
                if (!connectedIds.contains(nextId)) {
                    continue;
                }
                stageById.merge(nextId, currentStage + 1, Math::max);
                int remaining = indegrees.merge(nextId, -1, Integer::sum);
                if (remaining == 0) {
                    queue.addLast(nextId);
                }
            }
        }

        // 取得できたポケモンの進化進化ライン取得
        Map<Integer, List<Pokemon>> grouped = new LinkedHashMap<>();
        pokemonRepository.findAllById(stageById.keySet()).stream()
                .sorted(Comparator.comparingInt((Pokemon p) -> stageById.get(p.getId()))
                        .thenComparing(Pokemon::getId))
                .forEach(p -> grouped.computeIfAbsent(stageById.get(p.getId()),
                        key -> new ArrayList<>()).add(p));

        return new ArrayList<>(grouped.values());
    }

}