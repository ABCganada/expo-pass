import { useState } from "react";
import {
  useCreateManagerEventTicketMutation,
  useUpdateManagerEventTicketMutation,
  useDeleteManagerEventTicketMutation,
} from "../../api/managerEventDetailApi";
import {
  useCreateAdminEventTicketMutation,
  useUpdateAdminEventTicketMutation,
  useDeleteAdminEventTicketMutation,
} from "../../api/adminEventDetailApi";
import type { EventRole } from "../../types/eventRole";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { draftToCreatePayload, draftToUpdatePayload, type TicketDraft } from "./ticketUtils";

export function useTicketMutations(eventId: string, mode: EventRole) {
  const [createAsManager, { isLoading: isCreatingAsManager }] = useCreateManagerEventTicketMutation();
  const [updateAsManager, { isLoading: isUpdatingAsManager }] = useUpdateManagerEventTicketMutation();
  const [deleteAsManager] = useDeleteManagerEventTicketMutation();
  const [createAsAdmin, { isLoading: isCreatingAsAdmin }] = useCreateAdminEventTicketMutation();
  const [updateAsAdmin, { isLoading: isUpdatingAsAdmin }] = useUpdateAdminEventTicketMutation();
  const [deleteAsAdmin] = useDeleteAdminEventTicketMutation();
  const [error, setError] = useState<string | null>(null);

  const isCreating = mode === "admin" ? isCreatingAsAdmin : isCreatingAsManager;
  const isUpdating = mode === "admin" ? isUpdatingAsAdmin : isUpdatingAsManager;

  const create = async (draft: TicketDraft): Promise<boolean> => {
    setError(null);
    try {
      const createTicket = mode === "admin" ? createAsAdmin : createAsManager;
      await createTicket({ eventId, payload: draftToCreatePayload(draft) }).unwrap();
      return true;
    } catch (reason) {
      setError(queryErrorMessage(reason, "티켓 생성에 실패했습니다."));
      return false;
    }
  };

  const update = async (ticketId: string, draft: TicketDraft): Promise<boolean> => {
    setError(null);
    try {
      const updateTicket = mode === "admin" ? updateAsAdmin : updateAsManager;
      await updateTicket({ eventId, ticketId, payload: draftToUpdatePayload(draft) }).unwrap();
      return true;
    } catch (reason) {
      setError(queryErrorMessage(reason, "티켓 수정에 실패했습니다."));
      return false;
    }
  };

  const remove = async (ticketId: string): Promise<boolean> => {
    setError(null);
    try {
      const deleteTicket = mode === "admin" ? deleteAsAdmin : deleteAsManager;
      await deleteTicket({ eventId, ticketId }).unwrap();
      return true;
    } catch (reason) {
      setError(queryErrorMessage(reason, "티켓 삭제에 실패했습니다."));
      return false;
    }
  };

  return { create, update, remove, isCreating, isUpdating, error, setError };
}