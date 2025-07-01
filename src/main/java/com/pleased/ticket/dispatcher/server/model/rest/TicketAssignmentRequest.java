package com.pleased.ticket.dispatcher.server.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * TicketAssignmentRequest
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-06-29T19:58:10.280+02:00")


public class TicketAssignmentRequest   {
  @JsonProperty("ticketId")
  private String ticketId = null;

  @JsonProperty("assigneeId")
  private String assigneeId = null;

  public TicketAssignmentRequest ticketId(String ticketId) {
    this.ticketId = ticketId;
    return this;
  }

  /**
   * Get ticketId
   * @return ticketId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getTicketId() {
    return ticketId;
  }

  public void setTicketId(String ticketId) {
    this.ticketId = ticketId;
  }

  public TicketAssignmentRequest assigneeId(String assigneeId) {
    this.assigneeId = assigneeId;
    return this;
  }

  /**
   * Get assigneeId
   * @return assigneeId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(String assigneeId) {
    this.assigneeId = assigneeId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TicketAssignmentRequest ticketAssignmentRequest = (TicketAssignmentRequest) o;
    return Objects.equals(this.ticketId, ticketAssignmentRequest.ticketId) &&
        Objects.equals(this.assigneeId, ticketAssignmentRequest.assigneeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticketId, assigneeId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TicketAssignmentRequest {\n");
    
    sb.append("    ticketId: ").append(toIndentedString(ticketId)).append("\n");
    sb.append("    assigneeId: ").append(toIndentedString(assigneeId)).append("\n");
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

