package com.pleased.ticket.dispatcher.server.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * TicketStatusResponse
 */
@Validated
@javax.annotation.Generated(value = "com.glic.GentiSpringCodegen", date = "2025-06-29T19:58:10.280+02:00")


public class TicketStatusResponse   {
  @JsonProperty("ticketId")
  private String ticketId = null;

  @JsonProperty("status")
  private String status = null;

  @JsonProperty("updatedAt")
  private OffsetDateTime updatedAt = null;

  public TicketStatusResponse ticketId(String ticketId) {
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

  public TicketStatusResponse status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(value = "")


  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public TicketStatusResponse updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
  **/
  @ApiModelProperty(value = "")

  @Valid

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TicketStatusResponse ticketStatusResponse = (TicketStatusResponse) o;
    return Objects.equals(this.ticketId, ticketStatusResponse.ticketId) &&
        Objects.equals(this.status, ticketStatusResponse.status) &&
        Objects.equals(this.updatedAt, ticketStatusResponse.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticketId, status, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TicketStatusResponse {\n");
    
    sb.append("    ticketId: ").append(toIndentedString(ticketId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

