# 主题

主题只接受 `:root` 和 `.dark` 两个选择器中的 RecallHub Token。导入器拒绝其他选择器、At-rule、`url()`、`image-set()` 和 `expression()`。

普通颜色接受 OKLCH、HSL、RGB、HEX、`transparent` 和 `currentColor`。`--radius` 接受 `0`、`px`、`rem` 或 `em`。缺少的 RecallHub 语义色由前端回退值补齐。

