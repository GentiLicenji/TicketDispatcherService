package com.pleased.ticket.dispatcher.server.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * TicketAssignmentResponse
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-06-29T19:58:10.280+02:00")


public class TicketAssignmentResponse   {
  @JsonProperty("ticketId")
  private String ticketId = null;

  @JsonProperty("assignee")
  private TicketAssignmentResponseAssignee assignee = null;

  @JsonProperty("assignedAt")
  private OffsetDateTime assignedAt = null;

  public TicketAssignmentResponse ticketId(String ticketId) {
    this.ticketId = ticketId;
    return this;
  }

  /**
   * Get ticketId
   * @return ticketId
  **/
  @ApiModelProperty(value = "")


  public String getTicketId() {
    return ticketId;
  }

  public void setTicketId(String ticketId) {
    this.ticketId = ticketId;
  }

  public TicketAssignmentResponse assignee(TicketAssignmentResponseAssignee assignee) {
    this.assignee = assignee;
    return this;
  }

  /**
   * Get assignee
   * @return assignee
  **/
  @ApiModelProperty(value = "")

  @Valid

  public TicketAssignmentResponseAssignee getAssignee() {
    return assignee;
  }

  public void setAssignee(TicketAssignmentResponseAssignee assignee) {
    this.assignee = assignee;
  }

  public TicketAssignmentResponse assignedAt(OffsetDateTime assignedAt) {
    this.assignedAt = assignedAt;
    return this;
  }

  /**
   * Get assignedAt
   * @return assignedAt
  **/
  @ApiModelProperty(value = "")

  @Valid

  public OffsetDateTime getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(OffsetDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TicketAssignmentResponse ticketAssignmentResponse = (TicketAssignmentResponse) o;
    return Objects.equals(this.ticketId, ticketAssignmentResponse.ticketId) &&
        Objects.equals(this.assignee, ticketAssignmentResponse.assignee) &&
        Objects.equals(this.assignedAt, ticketAssignmentResponse.assignedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticketId, assignee, assignedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TicketAssignmentResponse {\n");
    
    sb.append("    ticketId: ").append(toIndentedString(ticketId)).append("\n");
    sb.append("    assignee: ").append(toIndentedString(assignee)).append("\n");
    sb.append("    assignedAt: ").append(toIndentedString(assignedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

