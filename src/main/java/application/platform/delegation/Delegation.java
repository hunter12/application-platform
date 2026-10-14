package application.platform.delegation;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.SQLRestriction;

import application.platform.common.BaseEntity;
import application.platform.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delegations")
@SQLRestriction("deleted_date is null")
public class Delegation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegator_id", nullable = false, foreignKey = @ForeignKey(name = "fk_delegations_delegator"))
    private AppUser delegator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegate_id", nullable = false, foreignKey = @ForeignKey(name = "fk_delegations_delegate"))
    private AppUser delegate;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 255)
    private String reason;

    private Instant cancelledDate;

    @Column(length = 100)
    private String cancelledBy;

    protected Delegation() {
    }

    public Delegation(AppUser delegator, AppUser delegate, LocalDate startDate, LocalDate endDate, String reason) {
        this.delegator = delegator;
        this.delegate = delegate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public DelegationStatus statusOn(LocalDate today) {
        if (cancelledDate != null) {
            return DelegationStatus.CANCELLED;
        }
        if (today.isBefore(startDate)) {
            return DelegationStatus.SCHEDULED;
        }
        return today.isAfter(endDate) ? DelegationStatus.COMPLETED : DelegationStatus.ACTIVE;
    }

    public void cancel(String cancelledBy) {
        this.cancelledDate = Instant.now();
        this.cancelledBy = cancelledBy;
    }

    public AppUser getDelegator() {
        return delegator;
    }

    public void setDelegator(AppUser delegator) {
        this.delegator = delegator;
    }

    public AppUser getDelegate() {
        return delegate;
    }

    public void setDelegate(AppUser delegate) {
        this.delegate = delegate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCancelledDate() {
        return cancelledDate;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }
}
