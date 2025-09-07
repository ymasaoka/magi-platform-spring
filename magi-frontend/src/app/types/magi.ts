export enum FinalDecisionEnum {
  Yes = "Yes",
  No = "No",
  Tie = "Tie",
}

export interface VoteOption {
  topic: string;
  yesCriteria: string;
  noCriteria: string;
}

export interface ResponseItem {
  personality: string;
  reason: string;
}

export interface MagiResponse {
  finalDecision: FinalDecisionEnum;
  countOfYes: number;
  countOfNo: number;
  yesResponses: ResponseItem[];
  noResponses: ResponseItem[];
}
