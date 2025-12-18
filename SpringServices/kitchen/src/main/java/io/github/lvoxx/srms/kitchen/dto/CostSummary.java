package io.github.lvoxx.srms.kitchen.dto;

import java.math.BigDecimal;

public record CostSummary(
                String itemCategory,
                BigDecimal totalCost) {
}
