import { useState } from "react";
import {
  useCreateManagerEventTicketMutation,
  useUpdateManagerEventTicketMutation,
  useDeleteManagerEventTicketMutation,
} from "../../api/managerEventDetailApi";
import { queryErrorMessage } from "@/features/store/api/queryError";
import { draftToCreatePayload, draftToUpdatePayload, type TicketDraft } from "./ticketUtils";

export function useTicketMutations(eventId: string) {
  const [createTicket, { isLoading: isCreating }] = useCreateManagerEventTicketMutation();
  const [updateTicket, { isLoading: isUpdating }] = useUpdateManagerEventTicketMutation();
  const [deleteTicket] = useDeleteManagerEventTicketMutation();
  const [error, setError] = useState<string | null>(null);

  const create = async (draft: TicketDraft): Promise<boolean> => {
    setError(null);
    try {
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
      await deleteTicket({ eventId, ticketId }).unwrap();
      return true;
    } catch (reason) {
      setError(queryErrorMessage(reason, "티켓 삭제에 실패했습니다."));
      return false;
    }
  };

  return { create, update, remove, isCreating, isUpdating, error, setError };
}