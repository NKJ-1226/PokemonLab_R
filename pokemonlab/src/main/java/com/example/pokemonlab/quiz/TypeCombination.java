
package com.example.pokemonlab.quiz;

import java.util.Objects;

/**
 * ポケモンのタイプの組み合わせを表す
 *
 * 単タイプ・複合タイプの両方に対応する
 * タイプの比較では、タイプ1とタイプ2の順序を区別しない
 *
 * @param type1 第一タイプ（必須）
 * @param type2 第二タイプ（単タイプの場合はnull）
 */
public record TypeCombination(String type1, String type2) {

    /**
     * タイプ情報を検証・正規化する
     */
    public TypeCombination {
        Objects.requireNonNull(type1, "タイプ1は必須です");

        if (type1.isBlank()) {
            throw new IllegalArgumentException(
                    "タイプ1は空にできません");
        }

        // 第二タイプが空文字の場合は単タイプとして扱う
        if (type2 != null && type2.isBlank()) {
            type2 = null;
        }
    }

    /**
     * 画面表示用のタイプ名を取得する
     *
     * @return 単タイプならタイプ1、複合タイプなら「タイプ1・タイプ2」
     */
    public String getDisplayName() {
        return type2 == null
                ? type1
                : type1 + "・" + type2;
    }

    /**
     * タイプの順序を区別せずに比較する
     *
     * @param obj 比較対象
     * @return 同じタイプの組み合わせならtrue
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TypeCombination other)) {
            return false;
        }

        // 同じ順序、または逆順で一致すれば同じ組み合わせ
        boolean sameOrder =
                Objects.equals(type1, other.type1)
                && Objects.equals(type2, other.type2);

        boolean reverseOrder =
                Objects.equals(type1, other.type2)
                && Objects.equals(type2, other.type1);

        return sameOrder || reverseOrder;
    }

    /**
     * タイプの順序に依存しないハッシュ値を返す
     *
     * @return タイプの組み合わせに対応するハッシュ値
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(type1)
                + Objects.hashCode(type2);
    }
}
