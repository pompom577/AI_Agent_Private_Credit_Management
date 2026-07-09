import { toast } from "sonner";

export function notifyDecisionConfirmed() {
  toast.success("Decision confirmed and permanently audited.");
}

export function notifyAuditWriteFailed() {
  toast.error("Action failed: Audit ledger unreachable. Please try again.");
}
