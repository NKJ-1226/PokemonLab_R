
package com.example.pokemonlab.quiz;

import java.util.Objects;

public record TypeCombination(String type1, String type2) {

    // DBに登録されたタイプの順序を維持
    public TypeCombination {
        Objects.requireNonNull(type1, "タイプ1は必須です。");

        if (type1.isBlank()) {
            throw new IllegalArgumentException("タイプ1は空にできません。");
        }

        if (type2 != null && type2.isBlank()) {
            type2 = null;
        }
    }

    // 画面表示用：タイプ1 → タイプ2
    public String getDisplayName() {
        return type2 == null
                ? type1
                : type1 + "・" + type2;
    }

    // 比較用：タイプの順序を無視
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TypeCombination other)) {
            return false;
        }

        return Objects.equals(type1, other.type1)
                && Objects.equals(type2, other.type2)
                ||
                Objects.equals(type1, other.type2)
                && Objects.equals(type2, other.type1);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type1)
                + Objects.hashCode(type2);
    }
}
