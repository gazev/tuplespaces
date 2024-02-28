from typing import List

from name_server.service_entry import ServiceEntry

from name_server.server_exceptions import (
    InvalidDeleteRequest,
    InvalidRegisterRequest,
    InvalidServiceEntry
)

class NameServerState:
    """ Class that manages the internal NameServer state, such as it's names table """
    def __init__(self):
        """
        Lookup table indexes ServiceEntries by name, each name can index various
        ServiceEntries, for example 
        self._name_lookup_table = {
            'TupleSpaces': [
                ServiceEntry(qual: A, addr: localhost:8000),
                ServiceEntry(qual: A, addr: localhost:8001)
                ServiceEntry(qual: B, addr: localhost:8000)
            ]
        }
        """
        self._name_lookup_table: Dict[str: List[ServiceEntry]] = {}

    def _exists_name(self, name: str) -> bool:
        """ Returns True if given ``name`` is present in the names table """
        return name in self._name_lookup_table

    def _exists_entry(self, name: str, service_entry: ServiceEntry):
        """ Returns True if a ServiceEntry indexed by ``name`` exists """
        if name not in self._name_lookup_table:
            return False

        # check if service entry is already registered
        for el in self._name_lookup_table[name]:
            if el.addr == service_entry.addr:
                return True
        
        return False
        
    def register(self, name: str, qual: str, addr: str) -> None:
        """ 
        Register a new ServiceEntry with ``qual`` qualifier and ``addr`` address indexed with ``name``.

        :param name: ServiceEntry name.
        :type name: str:

        :param qual: ServiceEntry qualifier.
        :type qual: str.

        :param addr: ServiceEntry address.
        :type addr: str.

        :return: None.

        :raises InvalidServiceEntry: If given ServiceEntry params are invalid.
        :raises InvalidRegisterRequest: If a service entry with given parameters already exists.
        """
        new_service_entry = ServiceEntry(qual, addr)

        # check if service already exists
        if self._exists_entry(name, new_service_entry):
            raise InvalidRegisterRequest(f'Service entry {name} {qual} {addr} already exists')

        # add to lookup table
        if self._name_lookup_table.get(name) is None:
            self._name_lookup_table[name] = [new_service_entry] # first entry with this name
        else:
            self._name_lookup_table[name].append(new_service_entry) # append to list of services with same name


    def lookup(self, name: str, qual: str) -> List[ServiceEntry]:
        """ 
        Get a list of ServiceEntries indexed by ``name`` and with ``qual`` as qualifier
        or simply indexed by ``name`` if ``id`` is not specified. 

        :params name: Desired ServiceEntries name.
        :type name: str.

        :params qual: Desired ServiceEntries qualifier
        :type qual: str.

        :returns: List of matching ServiceEntries.
        :rtype: List[ServiceEntry].
        """
        # service with given name doesn't exist
        if not self._exists_name(name):
            return []

        # get all service entries with given name and qual 
        if name and qual:
            # return list of all service entries addresses that match requested name and qualifier
            return [entry for entry in self._name_lookup_table[name] if entry.qual == qual]
        
        # get all service entries with given name (qual not specified)
        if name:
            return self._name_lookup_table[name]
        
        # no name specified (not normal behaviour)
        return []
   
    def delete(self, name: str, addr: str) -> None:
        """
        Deletes ServiceEntry indexed by ``name`` with given ``addr`` as address from names table. 

        :params name: Desired ServiceEntries name.
        :type name: str.

        :params addr: Desired ServiceEntry address.
        :type addr: str.

        :raises InvalidServiceEntry: If ``addr`` parameter is invalid.
        :raises InvalidDeleteRequest: If ServiceEntry with given parameter's doesn't exist.
        """
        # validate address, propagates exception
        ServiceEntry.is_valid_addr(addr)

        # check if name exists in names table
        if not self._exists_name(name):
            raise InvalidDeleteRequest(f"Service with name {name} doesn't exist")
        
        # remove ServiceEntry with given addr from names table
        for idx, val in enumerate(self._name_lookup_table[name]):
            if val.addr == addr:
                self._name_lookup_table[name].pop(idx)
                return
        
        raise InvalidDeleteRequest(f"Service with name {name} and address {addr} is not registered")

