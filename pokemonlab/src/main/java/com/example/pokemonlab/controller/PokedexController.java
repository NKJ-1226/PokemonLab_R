
package com.example.pokemonlab.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.example.pokemonlab.repository.PokemonEvolutionRepository;
import com.example.pokemonlab.repository.PokemonRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * ポケモン図鑑の一覧画面と詳細画面を管理するController
 *
 * 主な機能：
 * ・名前、世代、タイプによる検索
 * ・ページネーション
 * ・ポケモン詳細情報の表示
 * ・前後のポケモンへの移動
 * ・進化前、進化先、進化ラインの表示
 */
@Controller
public class PokedexController {

    /** 1ページに表示するポケモン数 */
    private static final int PAGE_SIZE = 20;

    /** 画面に同時表示するページ番号の最大数 */
    private static final int PAGE_NUMBER_DISPLAY_COUNT = 5;

    /**
     * タイプ検索で使用する選択肢
     *
     * DBに保存されているタイプ名と同じ表記を使用する
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

    /**
     * Repositoryを注入する
     *
     * @param pokemonRepository ポケモン情報のRepository
     * @param pokemonEvolutionRepository 進化関係のRepository
     */
    public PokedexController(
            PokemonRepository pokemonRepository,
            PokemonEvolutionRepository pokemonEvolutionRepository) {

        this.pokemonRepository = pokemonRepository;
        this.pokemonEvolutionRepository = pokemonEvolutionRepository;
    }

    /**
     * ポケモン図鑑の一覧画面を表示する
     *
     * 名前・世代・タイプによる絞り込みと、
     * ページネーションに対応する
     *
     * URL例：
     * /pokedex
     * /pokedex?name=ピカ
     * /pokedex?generation=1
     * /pokedex?type=でんき
     *
     * @param page 表示するページ番号（0から開始）
     * @param name 名前の検索文字列
     * @param generation 世代の検索条件
     * @param type タイプの検索条件
     * @param model 画面に渡すデータ
     * @return 図鑑一覧のテンプレート名
     */
    @GetMapping("/pokedex")
    public String pokedex(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Integer generation,
            @RequestParam(defaultValue = "") String type,
            Model model) {

        // 検索文字列の前後の空白を除去する
        String normalizedName = normalizeText(name);
        String normalizedType = normalizeText(type);

        // 負のページ番号が指定された場合は先頭ページに補正する
        int requestedPage = Math.max(page, 0);

        // 名前・世代・タイプの検索条件を作成する
        Specification<Pokemon> specification =
                createSearchSpecification(
                        normalizedName,
                        generation,
                        normalizedType);

        // 図鑑番号の昇順で並べ、同じ番号ならIDの昇順にする
        Sort sort = Sort.by(
                Sort.Order.asc("pokedexNo"),
                Sort.Order.asc("id"));

        Pageable pageable = PageRequest.of(
                requestedPage,
                PAGE_SIZE,
                sort);

        // 検索条件に一致するポケモンをページ単位で取得する
        Page<Pokemon> pokemonPage =
                pokemonRepository.findAll(specification, pageable);

        /*
         * 検索条件の変更などで存在しないページ番号が指定された場合、
         * 検索結果の最終ページに補正する
         *
         * 例：
         * 全件表示で50ページ目を閲覧
         * → 第1世代に絞り込む
         * → 50ページ目が存在しない
         * → 絞り込み結果の最終ページを表示
         */
        if (pokemonPage.getTotalPages() > 0
                && requestedPage >= pokemonPage.getTotalPages()) {

            requestedPage = pokemonPage.getTotalPages() - 1;

            pageable = PageRequest.of(
                    requestedPage,
                    PAGE_SIZE,
                    sort);

            pokemonPage = pokemonRepository.findAll(
                    specification,
                    pageable);
        }

        int currentPage = pokemonPage.getNumber();

        // ページネーションで表示するページ番号一覧
        List<Integer> pageNumbers = createPageNumbers(
                currentPage,
                pokemonPage.getTotalPages());

        // 現在のページで表示しているデータの開始位置
        long startItem = pokemonPage.isEmpty()
                ? 0
                : pokemonPage.getPageable().getOffset() + 1;

        // 現在のページで表示しているデータの終了位置
        long endItem = pokemonPage.isEmpty()
                ? 0
                : startItem + pokemonPage.getNumberOfElements() - 1;

        // 検索条件が1つでも指定されているかを判定する
        boolean searchActive =
                StringUtils.hasText(normalizedName)
                || generation != null
                || StringUtils.hasText(normalizedType);

        // 図鑑一覧とページネーションに必要な情報
        model.addAttribute("pokemonList", pokemonPage.getContent());
        model.addAttribute("pokemonPage", pokemonPage);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalElements", pokemonPage.getTotalElements());
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);

        // 検索フォームに現在の入力内容を戻すための情報
        model.addAttribute("searchName", normalizedName);
        model.addAttribute("selectedGeneration", generation);
        model.addAttribute("selectedType", normalizedType);
        model.addAttribute("pokemonTypes", POKEMON_TYPES);
        model.addAttribute("searchActive", searchActive);

        return "pokedex";
    }

    /**
     * 名前・世代・タイプの検索条件を作成する
     *
     * 条件が複数指定された場合はAND検索とする
     * タイプについては、第一タイプまたは第二タイプの
     * どちらかが一致すれば対象とする
     *
     * @param name 名前の部分一致検索条件
     * @param generation 世代の完全一致検索条件
     * @param type タイプの完全一致検索条件
     * @return 検索条件を表すSpecification
     */
    private Specification<Pokemon> createSearchSpecification(
            String name,
            Integer generation,
            String type) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // 名前の部分一致検索
            if (StringUtils.hasText(name)) {

                /*
                 * LIKE検索の特殊文字をエスケープする
                 * これにより、ユーザーが入力した%や_を
                 * ワイルドカードではなく通常の文字として扱う
                 */
                String escapedName =
                        escapeLikePattern(name.toLowerCase());

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + escapedName + "%",
                                '\\')
                );
            }

            // 世代の完全一致検索
            if (generation != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("generation"),
                                generation)
                );
            }

            // 第一タイプ・第二タイプのどちらかが一致すれば対象
            if (StringUtils.hasText(type)) {

                Predicate type1Matches =
                        criteriaBuilder.equal(
                                root.get("type1"),
                                type);

                Predicate type2Matches =
                        criteriaBuilder.equal(
                                root.get("type2"),
                                type);

                predicates.add(
                        criteriaBuilder.or(
                                type1Matches,
                                type2Matches)
                );
            }

            // 指定されたすべての検索条件をANDで結合する
            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * LIKE検索で特別な意味を持つ文字をエスケープする
     *
     * % は任意の文字列、_ は任意の1文字を表すため、
     * 検索文字列に含まれる場合は通常の文字として扱う
     *
     * @param text エスケープ対象の文字列
     * @return エスケープ後の文字列
     */
    private String escapeLikePattern(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 検索文字列を正規化する
     *
     * nullの場合は空文字列を返し、
     * それ以外の場合は前後の空白を除去する
     *
     * @param text 正規化する文字列
     * @return 正規化後の文字列
     */
    private String normalizeText(String text) {

        if (text == null) {
            return "";
        }

        return text.trim();
    }

    /**
     * ページネーションに表示するページ番号一覧を生成する
     *
     * 現在のページを中心に、最大5ページ分の番号を表示する
     * 最初や最後のページ付近では表示範囲を補正する
     *
     * @param currentPage 現在のページ番号（0から開始）
     * @param totalPages 全ページ数
     * @return 表示するページ番号一覧
     */
    private List<Integer> createPageNumbers(
            int currentPage,
            int totalPages) {

        if (totalPages <= 0) {
            return List.of();
        }

        int halfDisplayCount =
                PAGE_NUMBER_DISPLAY_COUNT / 2;

        // 現在のページを中心に表示開始位置を決める
        int startPage = Math.max(
                0,
                currentPage - halfDisplayCount);

        int endPage = Math.min(
                totalPages - 1,
                startPage + PAGE_NUMBER_DISPLAY_COUNT - 1);

        /*
         * 最終ページ付近で表示数が不足する場合、
         * 開始ページを前方へずらして可能な限り
         * 最大表示数を確保する
         */
        startPage = Math.max(
                0,
                endPage - PAGE_NUMBER_DISPLAY_COUNT + 1);

        return IntStream.rangeClosed(startPage, endPage)
                .boxed()
                .toList();
    }

    /**
     * 指定したポケモンの詳細画面を表示する
     *
     * 基本情報に加え、前後のポケモンと
     * 進化前・進化先・進化ラインの情報を取得する
     *
     * @param id 表示対象のポケモンID
     * @param model 画面に渡すデータ
     * @return 図鑑詳細のテンプレート名
     */
    @GetMapping("/pokedex/{id}")
    public String detail(
            @PathVariable Integer id,
            Model model) {

        // 指定IDのポケモンを取得する
        Pokemon pokemon = pokemonRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "指定されたポケモンが見つかりません。ID=" + id)
                );

        model.addAttribute("pokemon", pokemon);

        // ID順で直前のポケモンを取得する
        pokemonRepository.findFirstByIdLessThanOrderByIdDesc(id)
                .ifPresent(previous ->
                        model.addAttribute(
                                "previousPokemon",
                                previous)
                );

        // ID順で次のポケモンを取得する
        pokemonRepository.findFirstByIdGreaterThanOrderByIdAsc(id)
                .ifPresent(next ->
                        model.addAttribute(
                                "nextPokemon",
                                next)
                );

        // 現在のポケモンへ進化する関係を取得する
        List<PokemonEvolution> fromRelations =
                pokemonEvolutionRepository.findByToPokemonId(id);

        // 現在のポケモンから進化する関係を取得する
        List<PokemonEvolution> toRelations =
                pokemonEvolutionRepository.findByFromPokemonId(id);

        // 進化元のポケモンを取得する（分岐進化にも対応）
        List<Pokemon> evolutionFromList = fromRelations.stream()
                .map(relation -> pokemonRepository
                        .findById(relation.getFromPokemonId())
                        .orElse(null))
                .filter(evolutionFrom -> evolutionFrom != null)
                .toList();

        // 進化先のポケモンを取得する（分岐進化にも対応）
        List<Pokemon> evolutionToList = toRelations.stream()
                .map(relation -> pokemonRepository
                        .findById(relation.getToPokemonId())
                        .orElse(null))
                .filter(evolutionTo -> evolutionTo != null)
                .toList();

        model.addAttribute("evolutionFromList", evolutionFromList);
        model.addAttribute("evolutionToList", evolutionToList);

        // 進化関係をたどり、進化段階ごとにポケモンをまとめる
        List<List<Pokemon>> evolutionStages = createEvolutionStages(id);
        model.addAttribute("evolutionStages", evolutionStages);

        return "pokedex-detail";
    }

    /**
     * 指定ポケモンが属する進化ラインを作成する
     *
     * 進化関係をグラフとして扱い、以下の手順で構築する
     *
     * 1. 進化関係を前方向・後方向のマップに変換
     * 2. 指定ポケモンとつながる全ポケモンを探索
     * 3. 進化元が存在しないポケモンを起点にする
     * 4. 進化段階を計算してグループ化する
     *
     * 分岐進化や複数段階の進化に対応する
     *
     * @param pokemonId 表示対象のポケモンID
     * @return 進化段階ごとにまとめたポケモン一覧
     */
    private List<List<Pokemon>> createEvolutionStages(Integer pokemonId) {

        // DBからすべての進化関係を取得する
        List<PokemonEvolution> relations =
                pokemonEvolutionRepository.findAll();

        /*
         * nextMap:
         *   進化前ID → 進化先ID一覧
         *
         * previousMap:
         *   進化先ID → 進化前ID一覧
         *
         * 進化先が複数存在する分岐進化にも対応するため、
         * 値はList<Integer>で管理する
         */
        Map<Integer, List<Integer>> nextMap = new HashMap<>();
        Map<Integer, List<Integer>> previousMap = new HashMap<>();

        for (PokemonEvolution relation : relations) {

            Integer fromId = relation.getFromPokemonId();
            Integer toId = relation.getToPokemonId();

            nextMap.computeIfAbsent(
                    fromId,
                    key -> new ArrayList<>()
            ).add(toId);

            previousMap.computeIfAbsent(
                    toId,
                    key -> new ArrayList<>()
            ).add(fromId);
        }

        // 進化前も進化先も存在しない場合は空の一覧を返す
        if (!nextMap.containsKey(pokemonId)
                && !previousMap.containsKey(pokemonId)) {
            return List.of();
        }

        /*
         * 進化前・進化先の両方向をたどり、
         * 同じ進化ラインに属するすべてのIDを収集する
         *
         * 幅優先探索（BFS）を使用する
         * HashSetによって同じIDの重複探索を防ぐ
         */
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

                // 初めて見つかったポケモンだけ探索対象に追加する
                if (connectedIds.add(neighborId)) {
                    queue.addLast(neighborId);
                }
            }
        }

        /*
         * 進化前を持たないポケモンを起点として、
         * 進化段階を0から割り当てる
         */
        Map<Integer, Integer> stageById = new HashMap<>();
        queue.clear();

        for (Integer candidateId : connectedIds) {

            boolean hasPrevious =
                    previousMap.getOrDefault(candidateId, List.of())
                            .stream()
                            .anyMatch(connectedIds::contains);

            if (!hasPrevious) {
                stageById.put(candidateId, 0);
                queue.addLast(candidateId);
            }
        }

        /*
         * 起点が存在しない場合は処理を終了する
         * 循環した進化関係などによる無限ループを防ぐ
         */
        if (queue.isEmpty()) {
            return List.of();
        }

        /*
         * 各ポケモンに対して、進化元の数（入次数）を計算する
         *
         * 複数の進化元を持つ場合でも、
         * すべての進化元の処理が終わってから
         * 次の進化段階を確定できるようにする
         */
        Map<Integer, Integer> indegrees = new HashMap<>();

        for (Integer candidateId : connectedIds) {

            int degree = (int) previousMap
                    .getOrDefault(candidateId, List.of())
                    .stream()
                    .filter(connectedIds::contains)
                    .count();

            indegrees.put(candidateId, degree);
        }

        /*
         * 進化元から進化先へ順番にたどる
         *
         * 同じポケモンへ複数の進化経路がある場合は、
         * 最も大きい進化段階を採用する
         */
        while (!queue.isEmpty()) {

            Integer currentId = queue.removeFirst();
            int currentStage = stageById.get(currentId);

            for (Integer nextId :
                    nextMap.getOrDefault(currentId, List.of())) {

                if (!connectedIds.contains(nextId)) {
                    continue;
                }

                stageById.merge(nextId,currentStage + 1,Math::max);

                // 進化元を1件処理したので入次数を減らす
                int remaining = indegrees.merge(
                        nextId,
                        -1,
                        Integer::sum);

                // すべての進化元を処理したら次の探索対象に追加
                if (remaining == 0) {
                    queue.addLast(nextId);
                }
            }
        }

        /*
         * 進化段階の昇順、同じ段階ではIDの昇順に並べる
         *
         * LinkedHashMapを使うことで、
         * 進化段階ごとのグループの順序を維持する
         */
        Map<Integer, List<Pokemon>> grouped = new LinkedHashMap<>();

        pokemonRepository.findAllById(stageById.keySet())
                .stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (Pokemon pokemon) ->
                                                stageById.get(pokemon.getId()))
                                .thenComparing(Pokemon::getId)
                )
                .forEach(pokemon ->
                        grouped.computeIfAbsent(
                                stageById.get(pokemon.getId()),
                                key -> new ArrayList<>()
                        ).add(pokemon)
                );

        return new ArrayList<>(grouped.values());
    }
}
