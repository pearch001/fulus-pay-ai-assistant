package com.fulus.ai.assistant.enums;

/**
 * Categories for admin business insights queries
 * 
 * Used to classify and track the types of insights admins are requesting
 */
public enum InsightCategory {
    
    /**
     * Revenue, income, earnings analysis
     */
    REVENUE_ANALYSIS("Revenue Analysis", "Analyze revenue streams, MRR, growth rates, and financial performance"),
    
    /**
     * User growth, customer acquisition, active users
     */
    USER_GROWTH("User Growth", "Examine user acquisition, retention, churn, and growth metrics"),
    
    /**
     * Transaction patterns, payment volumes, success rates
     */
    TRANSACTION_PATTERNS("Transaction Patterns", "Study transaction volumes, patterns, success rates, and trends"),
    
    /**
     * Fee structures, pricing optimization, cost analysis
     */
    FEE_OPTIMIZATION("Fee Optimization", "Optimize pricing strategies, fee structures, and cost management"),
    
    /**
     * Risk assessment, fraud detection, security analysis
     */
    RISK_ASSESSMENT("Risk Assessment", "Evaluate risks, detect fraud patterns, and assess security measures"),
    
    /**
     * Market trends, competitive analysis, industry insights
     */
    MARKET_INTELLIGENCE("Market Intelligence", "Analyze market trends, competition, and industry positioning"),
    
    /**
     * General queries not fitting other categories
     */
    GENERAL_QUERY("General Query", "General business questions and platform inquiries");
    
    private final String displayName;
    private final String description;
    
    InsightCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

