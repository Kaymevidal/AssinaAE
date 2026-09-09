package com.kayomeira.assinatura.model;

public enum StatusAssinatura {
    PENDENTE("Pendente"),
    ASSINADO("Assinado"),
    REJEITADO("Rejeitado");
    
    private final String descricao;
    
    StatusAssinatura(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
