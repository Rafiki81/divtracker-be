package com.rafiki18.divtracker_be.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rafiki18.divtracker_be.model.MarketPriceTick;

public interface MarketPriceTickRepository extends JpaRepository<MarketPriceTick, UUID> {
}
