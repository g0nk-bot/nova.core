#!/bin/vbash
# Do not remove following line - VyOS-specific
source /opt/vyatta/etc/functions/script-template

# begin

### whats up with duplicate macs yo:
### find /sys/class/net -mindepth 1 -maxdepth 1 ! -name lo -printf "%P: " -execdir cat {}/address \;

{% for interface in interfaces %}
### {{ loop.index }} {{ 'hw_eth' + ( loop.index - 1 ) | string }} {{ vmware_tools_information.instance[ 'hw_eth' + ( loop.index - 1 ) | string ]['macaddress'] }}
MAC_ADDRESS="{{ vmware_tools_information.instance[ 'hw_eth' + ( loop.index - 1 ) | string ]['macaddress'] }}"
INTERFACE_NAME=$( ip addr | grep -B1 "permaddr $MAC_ADDRESS" | cut -f2 -d":" | grep eth | xargs | grep . || ip addr | grep -B1 "$MAC_ADDRESS" | cut -f2 -d":" | grep eth | xargs )
set interface ethernet $INTERFACE_NAME description '{{ interface.network_id }}'

{% for ip_address in interface.addresses %}

{% if ip_address.mode == 'ipv4_static' %}
set interface ethernet $INTERFACE_NAME address {{ ip_address.address }}
{% endif %}

{% if ip_address.mode == 'ipv6_static' %}
set interface ethernet $INTERFACE_NAME address {{ ip_address.address }}
{% endif %}

{% if (ip_address.mode == 'ipv4_static') and (ip_address.gateway is defined) and (no_gateway is not defined) and (ip_address.gateway != none) %}
set protocols static route 0.0.0.0/0 next-hop {{ ip_address.gateway }}
{% endif %}

{% if (ip_address.mode == 'ipv6_static') and (ip_address.gateway is defined) and (no_gateway is not defined) and (ip_address.gateway != none) %}
set protocols static route6 ::/0 next-hop {{ ip_address.gateway }}
{% endif %}

{% endfor %}

{% if (mgmt_ip != {}) and (interface.connection) %}
set interface ethernet $INTERFACE_NAME address {{ mgmt_ip }}
{% endif %}

{% endfor %}

{% if dns_servers is defined %}
{% for dns_server in dns_servers %}
set system name-server {{ dns_server }}
{% endfor %}
{% endif %}

{% if dns_servers6 is defined %}
{% for dns_server6 in dns_servers6 %}
set system name-server {{ dns_server6 }}
{% endfor %}
{% endif %}

set service ssh disable-host-validation

# Apply changes and save configuration
commit
save
exit

rm -f /home/vyos/firstconfig.sh