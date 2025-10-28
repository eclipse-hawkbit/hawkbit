DELETE FROM sp_tenant_configuration WHERE conf_key='action.cleanup.enabled';
UPDATE sp_tenant_configuration SET conf_key='action.cleanup.auto.expiry' WHERE conf_key='action.cleanup.actionExpiry';
UPDATE sp_tenant_configuration SET conf_key='action.cleanup.auto.expiry' WHERE conf_key='action.cleanup.auto.status';
UPDATE sp_tenant_configuration SET conf_key='actions.cleanup.onQuotaHit.percent' WHERE conf_key='action.cleanup.onQuotaHit.percent';